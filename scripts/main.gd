extends Control

const UI = preload("res://scripts/components/neon_ui.gd")
const DraggableScalable = preload("res://scripts/components/draggable_scalable.gd")
const TiltMover = preload("res://scripts/components/tilt_mover.gd")
const HoldToTrigger = preload("res://scripts/components/hold_to_trigger.gd")
const IdleTrigger = preload("res://scripts/components/idle_trigger.gd")

var current_level := 0
var max_unlocked := 1
var content: Control
var message_layer: Control
var hint_used := false

func _ready() -> void:
	set_process_input(true)
	show_home()

func clear_screen() -> void:
	for c in get_children():
		c.queue_free()

func make_background() -> void:
	var bg := ColorRect.new()
	bg.color = UI.BG
	bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(bg)

	for i in range(18):
		var dot := ColorRect.new()
		dot.color = [UI.CYAN, UI.PINK, UI.YELLOW][i % 3]
		dot.size = Vector2(5 + (i % 4) * 2, 5 + (i % 4) * 2)
		dot.position = Vector2(30 + ((i * 97) % 650), 80 + ((i * 149) % 1100))
		dot.mouse_filter = Control.MOUSE_FILTER_IGNORE
		bg.add_child(dot)

func show_home() -> void:
	clear_screen()
	make_background()
	var center := VBoxContainer.new()
	center.set_anchors_preset(Control.PRESET_CENTER)
	center.position = Vector2(110, 240)
	center.custom_minimum_size = Vector2(500, 720)
	center.alignment = BoxContainer.ALIGNMENT_CENTER
	center.add_theme_constant_override("separation", 28)
	add_child(center)

	var title := UI.label("NO RULES", 72, UI.CYAN)
	title.custom_minimum_size.y = 150
	center.add_child(title)
	center.add_child(UI.label("THINK WEIRD.", 34, UI.WHITE))

	var play := UI.button("PLAY", UI.YELLOW)
	play.pressed.connect(func(): start_level(1))
	center.add_child(play)

	var select := UI.button("LEVEL SELECT", UI.CYAN)
	select.pressed.connect(show_level_select)
	center.add_child(select)

	var beta := UI.label("ENGLISH BETA · 5 LEVELS", 20, Color(1,1,1,0.68))
	center.add_child(beta)

func show_level_select() -> void:
	clear_screen()
	make_background()
	var title := UI.label("LEVEL SELECT", 48, UI.WHITE)
	title.position = Vector2(90, 80)
	title.size = Vector2(540, 100)
	add_child(title)

	var grid := GridContainer.new()
	grid.columns = 2
	grid.position = Vector2(90, 230)
	grid.add_theme_constant_override("h_separation", 26)
	grid.add_theme_constant_override("v_separation", 26)
	add_child(grid)

	for i in range(1, 6):
		var b := UI.button(str(i), UI.CYAN if i % 2 else UI.PINK)
		b.custom_minimum_size = Vector2(255, 140)
		b.disabled = i > max_unlocked
		var level_num := i
		b.pressed.connect(func(): start_level(level_num))
		grid.add_child(b)

	var home := UI.button("BACK", UI.PINK)
	home.position = Vector2(245, 1040)
	home.pressed.connect(show_home)
	add_child(home)

func start_level(n: int) -> void:
	current_level = n
	hint_used = false
	clear_screen()
	make_background()
	build_shell()
	match n:
		1: build_level_1()
		2: build_level_2()
		3: build_level_3()
		4: build_level_4()
		5: build_level_5()

func build_shell() -> void:
	var title := UI.label("LEVEL %d" % current_level, 24, UI.WHITE)
	title.position = Vector2(260, 28)
	title.size = Vector2(200, 48)
	add_child(title)

	var menu := UI.button("MENU", UI.CYAN)
	menu.position = Vector2(28, 86)
	menu.custom_minimum_size = Vector2(180, 70)
	menu.pressed.connect(show_home)
	add_child(menu)

	var hint := UI.button("HINT", UI.YELLOW)
	hint.position = Vector2(512, 86)
	hint.custom_minimum_size = Vector2(180, 70)
	hint.pressed.connect(show_hint)
	add_child(hint)

	content = Control.new()
	content.position = Vector2.ZERO
	content.size = Vector2(720, 1280)
	content.mouse_filter = Control.MOUSE_FILTER_PASS
	add_child(content)

	message_layer = Control.new()
	message_layer.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	message_layer.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(message_layer)

func level_title(text: String) -> void:
	var box := Panel.new()
	box.position = Vector2(60, 180)
	box.size = Vector2(600, 120)
	box.add_theme_stylebox_override("panel", UI.panel(UI.DARK, 26, UI.CYAN, 3))
	content.add_child(box)
	var l := UI.label(text, 38, UI.WHITE)
	l.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	box.add_child(l)

func show_hint() -> void:
	hint_used = true
	var hints = {
		1: "Big problems sometimes need to become smaller.",
		2: "The phone itself can move things.",
		3: "A quick tap is not enough.",
		4: "Maybe doing nothing is still doing something.",
		5: "Why trust what the game tells you?"
	}
	toast(hints[current_level])

func toast(text: String) -> void:
	var p := Panel.new()
	p.position = Vector2(70, 970)
	p.size = Vector2(580, 150)
	p.add_theme_stylebox_override("panel", UI.panel(Color("#16143c"), 24, UI.PINK, 3))
	message_layer.add_child(p)
	var l := UI.label(text, 24, UI.WHITE)
	l.position = Vector2(24, 18)
	l.size = Vector2(532, 114)
	p.add_child(l)
	var t := get_tree().create_timer(3.0)
	t.timeout.connect(func():
		if is_instance_valid(p): p.queue_free()
	)

func win() -> void:
	max_unlocked = max(max_unlocked, min(5, current_level + 1))
	var shade := ColorRect.new()
	shade.color = Color(0.02, 0.01, 0.08, 0.88)
	shade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(shade)

	var card := Panel.new()
	card.position = Vector2(85, 370)
	card.size = Vector2(550, 460)
	card.add_theme_stylebox_override("panel", UI.panel(Color("#11102f"), 34, UI.YELLOW, 4))
	shade.add_child(card)

	var wow := UI.label("YOU GOT IT!", 50, UI.YELLOW)
	wow.position = Vector2(40, 45)
	wow.size = Vector2(470, 80)
	card.add_child(wow)

	var subtitle := UI.label("No rules. Just the right idea.", 24, UI.WHITE)
	subtitle.position = Vector2(55, 145)
	subtitle.size = Vector2(440, 70)
	card.add_child(subtitle)

	var next := UI.button("NEXT", UI.CYAN)
	next.position = Vector2(160, 245)
	next.custom_minimum_size = Vector2(230, 82)
	next.pressed.connect(func():
		if current_level < 5:
			start_level(current_level + 1)
		else:
			show_level_select()
	)
	card.add_child(next)

	var levels := UI.button("LEVELS", UI.PINK)
	levels.position = Vector2(160, 340)
	levels.custom_minimum_size = Vector2(230, 72)
	levels.pressed.connect(show_level_select)
	card.add_child(levels)

func build_level_1() -> void:
	level_title("KEY PROBLEM")
	var info := UI.label("OPEN THE LOCK", 28, UI.WHITE)
	info.position = Vector2(170, 320)
	info.size = Vector2(380, 60)
	content.add_child(info)

	var lock := Panel.new()
	lock.position = Vector2(500, 575)
	lock.size = Vector2(150, 200)
	lock.add_theme_stylebox_override("panel", UI.panel(Color("#ffcc21"), 26, Color.WHITE, 3))
	content.add_child(lock)
	var keyhole := ColorRect.new()
	keyhole.color = Color("#15112f")
	keyhole.position = Vector2(64, 84)
	keyhole.size = Vector2(22, 72)
	lock.add_child(keyhole)

	var key := DraggableScalable.new()
	key.position = Vector2(70, 505)
	key.size = Vector2(330, 150)
	key.pivot_offset = key.size * 0.5
	content.add_child(key)

	var head := Panel.new()
	head.position = Vector2(0, 15)
	head.size = Vector2(125, 125)
	head.add_theme_stylebox_override("panel", UI.panel(UI.PINK, 62, Color.WHITE, 3))
	key.add_child(head)
	var shaft := ColorRect.new()
	shaft.color = UI.PINK
	shaft.position = Vector2(105, 57)
	shaft.size = Vector2(200, 36)
	key.add_child(shaft)
	var tooth := ColorRect.new()
	tooth.color = UI.PINK
	tooth.position = Vector2(260, 80)
	tooth.size = Vector2(42, 50)
	key.add_child(tooth)

	key.dropped.connect(func(center: Vector2, s: float):
		var lock_rect := Rect2(lock.global_position - Vector2(25,25), lock.size + Vector2(50,50))
		if s <= 0.56 and lock_rect.has_point(center):
			win()
		else:
			if s > 0.56: toast("Still too big.")
	)

func build_level_2() -> void:
	level_title("GRAVITY STAR")
	var info := UI.label("REACH THE STAR", 28, UI.WHITE)
	info.position = Vector2(170, 320)
	info.size = Vector2(380, 60)
	content.add_child(info)

	var arena := Panel.new()
	arena.position = Vector2(60, 405)
	arena.size = Vector2(600, 650)
	arena.add_theme_stylebox_override("panel", UI.panel(Color("#0d0b34"), 30, UI.CYAN, 3))
	content.add_child(arena)

	var goal := Label.new()
	goal.text = "★"
	goal.position = Vector2(475, 55)
	goal.size = Vector2(90, 90)
	goal.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	goal.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	goal.add_theme_font_size_override("font_size", 76)
	goal.add_theme_color_override("font_color", UI.YELLOW)
	arena.add_child(goal)

	var ball := TiltMover.new()
	ball.position = Vector2(90, 500)
	ball.size = Vector2(72,72)
	ball.bounds = Rect2(Vector2.ZERO, arena.size)
	arena.add_child(ball)
	var orb := Panel.new()
	orb.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	orb.add_theme_stylebox_override("panel", UI.panel(UI.PINK, 36, Color.WHITE, 3))
	ball.add_child(orb)

	var watcher := Timer.new()
	watcher.wait_time = 0.08
	watcher.autostart = true
	arena.add_child(watcher)
	watcher.timeout.connect(func():
		if Rect2(goal.position, goal.size).intersects(Rect2(ball.position, ball.size)):
			watcher.stop()
			win()
	)

func build_level_3() -> void:
	level_title("POP!")
	var info := UI.label("MAKE IT POP", 28, UI.WHITE)
	info.position = Vector2(170, 320)
	info.size = Vector2(380, 60)
	content.add_child(info)

	var balloon := HoldToTrigger.new()
	balloon.text = ""
	balloon.position = Vector2(210, 470)
	balloon.size = Vector2(300, 390)
	balloon.hold_seconds = 2.8
	balloon.focus_mode = Control.FOCUS_NONE
	balloon.add_theme_stylebox_override("normal", UI.panel(UI.PINK, 145, Color.WHITE, 4))
	balloon.add_theme_stylebox_override("pressed", UI.panel(UI.PINK.lightened(0.08), 145, UI.YELLOW, 5))
	content.add_child(balloon)

	var string := ColorRect.new()
	string.color = UI.WHITE
	string.position = Vector2(358, 850)
	string.size = Vector2(5, 150)
	content.add_child(string)

	balloon.progress_changed.connect(func(p: float):
		var s := 1.0 + p * 0.45
		balloon.pivot_offset = balloon.size * 0.5
		balloon.scale = Vector2.ONE * s
	)
	balloon.completed.connect(func():
		for i in range(24):
			var spark := ColorRect.new()
			spark.color = [UI.PINK, UI.CYAN, UI.YELLOW][i % 3]
			spark.position = balloon.position + balloon.size * 0.5
			spark.size = Vector2(12,12)
			content.add_child(spark)
			var tw := create_tween()
			var angle := TAU * float(i) / 24.0
			tw.tween_property(spark, "position", spark.position + Vector2(cos(angle), sin(angle)) * (120 + (i%5)*30), 0.35)
			tw.tween_callback(spark.queue_free)
		balloon.visible = false
		get_tree().create_timer(0.35).timeout.connect(win)
	)

func build_level_4() -> void:
	level_title("ZEN DOOR")
	var info := UI.label("OPEN THE DOOR", 28, UI.WHITE)
	info.position = Vector2(170, 320)
	info.size = Vector2(380, 60)
	content.add_child(info)

	var door := Panel.new()
	door.position = Vector2(220, 445)
	door.size = Vector2(280, 510)
	door.pivot_offset = Vector2(0, 255)
	door.add_theme_stylebox_override("panel", UI.panel(Color("#7848ff"), 22, UI.PINK, 4))
	content.add_child(door)

	var knob := Panel.new()
	knob.position = Vector2(220, 240)
	knob.size = Vector2(34,34)
	knob.add_theme_stylebox_override("panel", UI.panel(UI.YELLOW, 17, Color.WHITE, 2))
	door.add_child(knob)

	var idle := IdleTrigger.new()
	idle.idle_seconds = 7.0
	content.add_child(idle)

	var calm := UI.label("...", 42, Color(1,1,1,0.72))
	calm.position = Vector2(260, 1010)
	calm.size = Vector2(200, 70)
	content.add_child(calm)

	idle.completed.connect(func():
		var tw := create_tween()
		tw.tween_property(door, "scale:x", 0.05, 0.7)
		tw.tween_callback(win)
	)

	var touch_guard := Control.new()
	touch_guard.position = Vector2(0, 300)
	touch_guard.size = Vector2(720, 900)
	touch_guard.mouse_filter = Control.MOUSE_FILTER_PASS
	content.add_child(touch_guard)
	touch_guard.gui_input.connect(func(event: InputEvent):
		if event is InputEventScreenTouch or event is InputEventMouseButton:
			if event.pressed:
				idle.reset()
				calm.text = "..."
	)

func build_level_5() -> void:
	level_title("LIAR CHIBI")
	var liar := UI.label("PRESS LEFT.\nTRUST ME.", 42, UI.PINK)
	liar.position = Vector2(110, 350)
	liar.size = Vector2(500, 170)
	content.add_child(liar)

	var face := Panel.new()
	face.position = Vector2(250, 535)
	face.size = Vector2(220, 220)
	face.add_theme_stylebox_override("panel", UI.panel(UI.YELLOW, 110, Color.WHITE, 4))
	content.add_child(face)
	var eyes := UI.label("◕  ◕", 54, Color("#171328"))
	eyes.position = Vector2(20, 42)
	eyes.size = Vector2(180, 70)
	face.add_child(eyes)
	var mouth := UI.label("⌣", 58, Color("#171328"))
	mouth.position = Vector2(55, 105)
	mouth.size = Vector2(110, 70)
	face.add_child(mouth)

	var left := UI.button("LEFT", UI.PINK)
	left.position = Vector2(75, 865)
	left.custom_minimum_size = Vector2(260, 100)
	content.add_child(left)
	var right := UI.button("RIGHT", UI.CYAN)
	right.position = Vector2(385, 865)
	right.custom_minimum_size = Vector2(260, 100)
	content.add_child(right)

	left.pressed.connect(func():
		liar.text = "SEE? I LIED."
		var tw := create_tween()
		for dx in [18,-18,14,-14,8,-8,0]:
			tw.tween_property(left, "position:x", 75 + dx, 0.05)
	)
	right.pressed.connect(win)
