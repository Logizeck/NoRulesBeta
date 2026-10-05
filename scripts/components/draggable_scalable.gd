extends Control
class_name DraggableScalable

signal dropped(global_center: Vector2, current_scale: float)

@export var min_scale := 0.32
@export var max_scale := 1.15
@export var drag_enabled := true
@export var scale_enabled := true

var _touches: Dictionary = {}
var _drag_offset := Vector2.ZERO
var _drag_finger := -1
var _last_pinch_distance := 0.0

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_STOP

func _gui_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed:
			_touches[event.index] = event.position
			if _touches.size() == 1 and drag_enabled:
				_drag_finger = event.index
				_drag_offset = global_position - event.position
			elif _touches.size() == 2:
				_last_pinch_distance = _pinch_distance()
		else:
			_touches.erase(event.index)
			if event.index == _drag_finger:
				_drag_finger = -1
				dropped.emit(global_position + size * scale * 0.5, scale.x)
			if _touches.size() < 2:
				_last_pinch_distance = 0.0

	elif event is InputEventScreenDrag:
		_touches[event.index] = event.position
		if _touches.size() >= 2 and scale_enabled:
			var d := _pinch_distance()
			if _last_pinch_distance > 0.0:
				var factor := d / _last_pinch_distance
				var target := clamp(scale.x * factor, min_scale, max_scale)
				scale = Vector2.ONE * target
			_last_pinch_distance = d
		elif event.index == _drag_finger and drag_enabled:
			global_position = event.position + _drag_offset

	elif event is InputEventMouseButton:
		if event.button_index == MOUSE_BUTTON_LEFT:
			if event.pressed:
				_drag_finger = 0
				_drag_offset = global_position - event.global_position
			else:
				_drag_finger = -1
				dropped.emit(global_position + size * scale * 0.5, scale.x)

	elif event is InputEventMouseMotion and _drag_finger == 0 and drag_enabled:
		global_position = event.global_position + _drag_offset

func _pinch_distance() -> float:
	if _touches.size() < 2:
		return 0.0
	var vals := _touches.values()
	return (vals[0] as Vector2).distance_to(vals[1] as Vector2)
