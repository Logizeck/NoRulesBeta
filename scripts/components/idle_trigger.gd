extends Node
class_name IdleTrigger

signal progress_changed(value: float)
signal completed

@export var idle_seconds := 7.0
var elapsed := 0.0
var done := false

func reset() -> void:
	if not done:
		elapsed = 0.0
		progress_changed.emit(0.0)

func _process(delta: float) -> void:
	if done:
		return
	elapsed += delta
	var p := clamp(elapsed / idle_seconds, 0.0, 1.0)
	progress_changed.emit(p)
	if p >= 1.0:
		done = true
		completed.emit()
