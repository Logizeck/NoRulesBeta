extends Button
class_name HoldToTrigger

signal progress_changed(value: float)
signal completed

@export var hold_seconds := 2.5
var _holding := false
var _elapsed := 0.0
var _done := false

func _ready() -> void:
	button_down.connect(func(): _holding = true)
	button_up.connect(func(): _holding = false)

func _process(delta: float) -> void:
	if _done:
		return
	if _holding:
		_elapsed += delta
	else:
		_elapsed = max(0.0, _elapsed - delta * 0.65)
	var p := clamp(_elapsed / hold_seconds, 0.0, 1.0)
	progress_changed.emit(p)
	if p >= 1.0:
		_done = true
		completed.emit()
