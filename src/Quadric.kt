import vision.gears.webglmath.Mat4
import vision.gears.webglmath.Vec4
import vision.gears.webglmath.UniformProvider

class Quadric(
	id : Int,
	vararg programs : Program) : UniformProvider("quadrics[$id]") {

		val surface     by QuadraticMat4()
		val clipper    	by QuadraticMat4()
		val color   	by Vec4()
		val reflectance by Vec4()

	init{
	addComponentsAndGatherUniforms(*programs)
	}

	companion object {
		val sphere = QuadraticMat4(
				1.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 1.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 1.0f, 0.0f,
				0.0f, 0.0f, 0.0f,-1.0f)
		val all = QuadraticMat4(
				0.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f,-1.0f)
		val cylinder = QuadraticMat4(
				1.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 1.0f, 0.0f,
				0.0f, 0.0f, 0.0f,-1.0f)
		val cone = QuadraticMat4(
				1.0f, 0.0f, 0.0f, 0.0f,
				0.0f, -1.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 1.0f, 0.0f,
				0.0f, 0.0f, 0.0f, 0.0f)
		val paraboloid = QuadraticMat4(
				1.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f, -0.5f,
				0.0f, 0.0f, 1.0f, 0.0f,
				0.0f, -0.5f, 0.0f, 0.0f)
		val hyperboloid = QuadraticMat4(
				1.0f, 0.0f, 0.0f, 0.0f,
				0.0f, -1.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 1.0f, 0.0f,
				0.0f, 0.0f, 0.0f, -1.0f)
		val doubleplain = QuadraticMat4(
				0.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 1.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f, -1.0f)
  }
}
