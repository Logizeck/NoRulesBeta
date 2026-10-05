extends Control
class_name TiltMover

@export var speed := 120.0
@export var damping := 0.92
@export var bounds := Rect2(60, 300, 600, 700)

var velocity := Vector2.ZERO
var keyboard_fallback := true

func _process(delta: float) -> void:
	var a := Input.get_accelerometer()
	var force := Vector2(a.x, -a.y)
	if force.length() < 0.08 and keyboard_fallback:
		force = Input.get_vector("ui_left", "ui_right", "ui_up", "ui_down") * 5.0
	velocity += force * speed * delta
	velocity *= pow(damping, delta * 60.0)
	position += velocity * delta

	position.x = clamp(position.x, bounds.position.x, bounds.end.x - size.x)
	position.y = clamp(position.y, bounds.position.y, bounds.end.y - size.y)
