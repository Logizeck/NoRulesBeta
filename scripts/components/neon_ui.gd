extends RefCounted
class_name NeonUI

const BG := Color("#07051f")
const CYAN := Color("#16d9ff")
const PINK := Color("#ff2bbf")
const YELLOW := Color("#fff200")
const WHITE := Color("#f7f7ff")
const DARK := Color("#11102f")

static func panel(color: Color = DARK, radius: int = 28, border: Color = CYAN, border_width: int = 3) -> StyleBoxFlat:
	var s := StyleBoxFlat.new()
	s.bg_color = color
	s.corner_radius_top_left = radius
	s.corner_radius_top_right = radius
	s.corner_radius_bottom_left = radius
	s.corner_radius_bottom_right = radius
	s.border_width_left = border_width
	s.border_width_top = border_width
	s.border_width_right = border_width
	s.border_width_bottom = border_width
	s.border_color = border
	s.shadow_color = Color(border, 0.25)
	s.shadow_size = 14
	return s

static func button(text: String, color: Color = CYAN) -> Button:
	var b := Button.new()
	b.text = text
	b.custom_minimum_size = Vector2(230, 84)
	b.add_theme_font_size_override("font_size", 28)
	b.add_theme_color_override("font_color", Color("#07101c"))
	b.add_theme_color_override("font_hover_color", Color("#07101c"))
	b.add_theme_stylebox_override("normal", panel(color, 24, Color.WHITE, 2))
	var hover := panel(color.lightened(0.08), 24, Color.WHITE, 4)
	b.add_theme_stylebox_override("hover", hover)
	var pressed := panel(color.darkened(0.12), 24, Color.WHITE, 2)
	b.add_theme_stylebox_override("pressed", pressed)
	return b

static func label(text: String, size: int = 34, color: Color = WHITE) -> Label:
	var l := Label.new()
	l.text = text
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	l.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	l.add_theme_font_size_override("font_size", size)
	l.add_theme_color_override("font_color", color)
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	return l

static func rounded_rect(color: Color, size: Vector2, radius: int = 24, border: Color = Color.WHITE, border_width: int = 3) -> Panel:
	var p := Panel.new()
	p.custom_minimum_size = size
	p.add_theme_stylebox_override("panel", panel(color, radius, border, border_width))
	return p
