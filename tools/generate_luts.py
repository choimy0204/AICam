"""
필터 LUT 생성기.

각 필터를 색온도·틴트·채도·대비·커브(섀도/하이라이트)·페이드 파라미터로 정의하고,
64x64x64 3D LUT를 512x512 PNG(8x8 타일)로 저장한다. 외부 라이브러리 없이 표준 라이브러리만 쓴다.

PNG 배치: 파랑(b) 값마다 64x64 타일 하나. 타일 위치 = (b % 8, b // 8),
타일 안에서 x = 빨강(r), y = 초록(g). 앱의 LutLoader가 같은 규칙으로 읽는다.

실행: python tools/generate_luts.py   →  app/src/main/assets/luts/<id>.png
"""

import os
import struct
import zlib
from dataclasses import dataclass

LUT_SIZE = 64
TILES_PER_ROW = 8
IMAGE_SIZE = LUT_SIZE * TILES_PER_ROW  # 512

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "luts")


@dataclass
class Filter:
    id: str
    temperature: float = 0.0   # +: 따뜻하게(R↑ B↓), -: 차갑게
    tint: float = 0.0          # +: 마젠타 쪽(G↓), -: 초록 쪽
    saturation: float = 1.0    # 1.0 = 그대로
    contrast: float = 1.0      # 1.0 = 그대로, 0.5 기준으로 늘이고 줄인다
    shadows: float = 0.0       # +: 어두운 영역을 들어 올린다
    highlights: float = 0.0    # -: 밝은 영역을 눌러 준다
    fade: float = 0.0          # 검은색을 이만큼 회색 쪽으로 띄운다 (필름 페이드)
    monochrome: bool = False
    # 스플릿 톤: 어두운 영역/밝은 영역에 더할 (R, G, B). 필름 느낌(예: 섀도 청록, 하이라이트 주황)을 낸다.
    shadow_tint: tuple = (0.0, 0.0, 0.0)
    highlight_tint: tuple = (0.0, 0.0, 0.0)


# ── 필터 정의 표 ─────────────────────────────────────────────────────────────
# 그레인·비네팅은 셰이더 파라미터라 앱의 FilterCatalog.kt에 있다.
FILTERS = [
    #       id              temp   tint   sat   contrast shadows highlights fade
    Filter("clear_skin",    0.02,  0.02,  0.92, 0.92,    0.06,   0.00,      0.02),
    Filter("warm_film",     0.08,  0.01,  0.90, 1.08,    0.04,  -0.04,      0.06),
    Filter("cool_film",    -0.07, -0.01,  0.88, 1.06,    0.03,  -0.03,      0.05),
    Filter("pastel",        0.01,  0.02,  0.75, 0.85,    0.10,   0.03,      0.10),
    Filter("vintage_fade",  0.05, -0.02,  0.70, 0.90,    0.05,  -0.06,      0.14),
    Filter("mono",          0.00,  0.00,  0.00, 1.15,    0.00,   0.00,      0.03, monochrome=True),
    Filter("vivid",         0.01,  0.00,  1.30, 1.08,    0.00,   0.00,      0.00),
    Filter("food_warm",     0.07,  0.00,  1.15, 1.05,    0.03,   0.00,      0.00),
    Filter("food_crisp",    0.02,  0.00,  1.20, 1.15,   -0.02,   0.03,      0.00),
    Filter("cafe_mood",     0.04,  0.00,  0.80, 0.95,    0.06,  -0.05,      0.08),
    # ── 요즘 유행 필터 (사전 조사: docs/DECISIONS.md "유행 필터 추가") ──
    # 밀크: 대비를 낮추고 섀도를 띄운 뽀얀 우유빛 (한국 감성 카메라 앱의 milk 톤)
    Filter("milk",          0.01,  0.03,  0.82, 0.82,    0.12,   0.04,      0.08,
           highlight_tint=(0.02, 0.01, 0.01)),
    # 버터: 노르스름하고 크리미한 따뜻한 톤
    Filter("butter",        0.07, -0.01,  0.88, 0.90,    0.08,  -0.02,      0.06,
           highlight_tint=(0.03, 0.02, -0.02)),
    # 인물 필름: 코닥 포트라 400 계열. 피부는 따뜻하게, 채도는 살짝 낮게, 하이라이트는 부드럽게
    Filter("portra",        0.05,  0.02,  0.90, 0.94,    0.05,  -0.06,      0.04,
           shadow_tint=(-0.01, 0.00, 0.02), highlight_tint=(0.03, 0.01, -0.01)),
    # 골드 필름: 코닥 골드 200 계열. 노랑·주황이 진한 여름 햇살 톤
    Filter("gold_film",     0.10, -0.02,  1.08, 1.05,    0.03,  -0.04,      0.04,
           highlight_tint=(0.03, 0.02, -0.03)),
    # 크롬: 후지 클래식 크롬 계열. 채도는 낮고 대비는 단단하게, 섀도는 청록
    Filter("chrome",        0.01,  0.00,  0.72, 1.12,    0.00,  -0.05,      0.03,
           shadow_tint=(-0.02, 0.01, 0.03), highlight_tint=(0.02, 0.01, -0.01)),
    # 디카 Y2K: 2000년대 똑딱이 디카 + 플래시. 대비·채도가 높고 살짝 차갑다
    Filter("digicam",      -0.04, -0.02,  1.12, 1.18,   -0.02,   0.05,      0.00,
           shadow_tint=(0.00, 0.01, 0.02)),
    # 청량: 음료·디저트용 맑고 시원한 톤 (음식 카메라 앱의 '청량한' 계열)
    Filter("food_fresh",   -0.04, -0.01,  1.10, 1.04,    0.04,   0.04,      0.00,
           shadow_tint=(-0.01, 0.01, 0.02)),
]


def clamp01(value):
    return min(1.0, max(0.0, value))


def luma(r, g, b):
    return 0.299 * r + 0.587 * g + 0.114 * b


def apply_filter(f, r, g, b):
    """0~1 범위 RGB 한 점에 필터를 적용한다. 순서: 색온도 → 틴트 → 대비 → 커브 → 채도 → 스플릿 톤 → 페이드."""
    r *= 1.0 + f.temperature
    b *= 1.0 - f.temperature
    g *= 1.0 - f.tint

    r = (r - 0.5) * f.contrast + 0.5
    g = (g - 0.5) * f.contrast + 0.5
    b = (b - 0.5) * f.contrast + 0.5

    def curve(x):
        x = clamp01(x)
        return x + f.shadows * (1.0 - x) ** 2 + f.highlights * x ** 2

    r, g, b = curve(r), curve(g), curve(b)

    y = luma(r, g, b)
    if f.monochrome:
        r = g = b = y
    else:
        r = y + (r - y) * f.saturation
        g = y + (g - y) * f.saturation
        b = y + (b - y) * f.saturation

    shadow_weight = (1.0 - y) ** 2
    highlight_weight = y ** 2
    r += f.shadow_tint[0] * shadow_weight + f.highlight_tint[0] * highlight_weight
    g += f.shadow_tint[1] * shadow_weight + f.highlight_tint[1] * highlight_weight
    b += f.shadow_tint[2] * shadow_weight + f.highlight_tint[2] * highlight_weight

    r = f.fade + clamp01(r) * (1.0 - f.fade)
    g = f.fade + clamp01(g) * (1.0 - f.fade)
    b = f.fade + clamp01(b) * (1.0 - f.fade)
    return clamp01(r), clamp01(g), clamp01(b)


def build_lut_pixels(f):
    """512x512 RGB 바이트(행 단위 bytearray 리스트)를 만든다."""
    rows = [bytearray(IMAGE_SIZE * 3) for _ in range(IMAGE_SIZE)]
    step = 1.0 / (LUT_SIZE - 1)
    for bi in range(LUT_SIZE):
        tile_x = (bi % TILES_PER_ROW) * LUT_SIZE
        tile_y = (bi // TILES_PER_ROW) * LUT_SIZE
        for gi in range(LUT_SIZE):
            row = rows[tile_y + gi]
            for ri in range(LUT_SIZE):
                out = apply_filter(f, ri * step, gi * step, bi * step)
                offset = (tile_x + ri) * 3
                row[offset] = round(out[0] * 255)
                row[offset + 1] = round(out[1] * 255)
                row[offset + 2] = round(out[2] * 255)
    return rows


def write_png(path, rows, width, height):
    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + bytes(row) for row in rows)  # 필터 타입 0(None)
    header = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)  # 8bit RGB
    with open(path, "wb") as out:
        out.write(b"\x89PNG\r\n\x1a\n")
        out.write(chunk(b"IHDR", header))
        out.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        out.write(chunk(b"IEND", b""))


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    for f in FILTERS:
        path = os.path.join(OUTPUT_DIR, f.id + ".png")
        write_png(path, build_lut_pixels(f), IMAGE_SIZE, IMAGE_SIZE)
        print("생성:", os.path.normpath(path))


if __name__ == "__main__":
    main()
