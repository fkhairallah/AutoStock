"""Generate Google Play store graphic assets for AutoStock."""
import math
from PIL import Image, ImageDraw, ImageFont

BG_BLUE = (13, 71, 161)       # #0D47A1
WHITE   = (255, 255, 255)
WHITE44 = (255, 255, 255, 112) # 44% alpha fill

# ---------------------------------------------------------------------------
# Shared: draw the stock trend chart onto any canvas
# ---------------------------------------------------------------------------
def draw_chart(draw, pts, scale, ox=0, oy=0, stroke=4, dot_r=4):
    """pts = list of (x,y) in 108-unit space. scale maps to canvas pixels."""
    def t(x, y):
        return (ox + x * scale, oy + y * scale)

    # filled area
    poly = [t(22, 84)] + [t(x, y) for x, y in pts] + [t(86, 84)]
    draw.polygon(poly, fill=WHITE44)

    # trend line
    line_pts = [t(x, y) for x, y in pts]
    draw.line(line_pts, fill=WHITE, width=round(stroke * scale))

    # dots
    for x, y in pts:
        cx, cy = t(x, y)
        r = dot_r * scale
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=WHITE)


CHART_PTS = [(22, 75), (36, 62), (50, 68), (64, 48), (78, 38), (86, 26)]


# ---------------------------------------------------------------------------
# 1. App icon  512 × 512
# ---------------------------------------------------------------------------
def make_icon(path="icon_512.png"):
    size = 512
    scale = size / 108

    img = Image.new("RGBA", (size, size), BG_BLUE)
    draw = ImageDraw.Draw(img, "RGBA")
    draw_chart(draw, CHART_PTS, scale, stroke=3.5, dot_r=3.5)

    img.save(path)
    print(f"Saved {path}")


# ---------------------------------------------------------------------------
# 2. Feature graphic  1024 × 500
# ---------------------------------------------------------------------------
def make_feature(path="feature_graphic.png"):
    W, H = 1024, 500
    img = Image.new("RGB", (W, H), BG_BLUE)
    draw = ImageDraw.Draw(img, "RGBA")

    # chart centred on right half, scaled up
    chart_h = 320
    scale = chart_h / 108
    ox = W // 2 - 20
    oy = (H - chart_h) // 2 + 10
    draw_chart(draw, CHART_PTS, scale, ox=ox, oy=oy, stroke=3.5, dot_r=3.5)

    # gradient-style dark overlay on left so text is readable
    for x in range(W // 2 + 80):
        alpha = max(0, 180 - int(180 * x / (W // 2 + 80)))
        draw.line([(x, 0), (x, H)], fill=(0, 0, 0, alpha))

    # App name
    try:
        font_title = ImageFont.truetype("arial.ttf", 72)
        font_sub   = ImageFont.truetype("arial.ttf", 30)
    except Exception:
        font_title = ImageFont.load_default(size=72)
        font_sub   = ImageFont.load_default(size=30)

    draw2 = ImageDraw.Draw(img)
    draw2.text((60, 160), "AutoStock", font=font_title, fill=WHITE)
    draw2.text((62, 248), "Real-time quotes for Android Auto", font=font_sub,
               fill=(180, 210, 255))

    img.save(path)
    print(f"Saved {path}")


if __name__ == "__main__":
    import os
    out = r"C:\Users\fady\Documents\Development\AutoStock\store_assets"
    make_icon(os.path.join(out, "icon_512.png"))
    make_feature(os.path.join(out, "feature_graphic.png"))
    print("Done.")
