import org.w3c.dom.HTMLCanvasElement
import vision.gears.webglmath.*
import org.khronos.webgl.WebGLRenderingContext as GL
import kotlin.js.Date

class Scene (
  val gl : WebGL2RenderingContext) : UniformProvider("scene"){

  val vsQuad = Shader(gl, GL.VERTEX_SHADER, "shaders/quad-vs.glsl")
  val fsTrace = Shader(gl, GL.FRAGMENT_SHADER, "shaders/trace-fs.glsl")
  val fsDisplay = Shader(gl, GL.FRAGMENT_SHADER, "shaders/display-fs.glsl")
  val traceProgram = Program(gl, vsQuad, fsTrace)
  val displayProgram = Program(gl, vsQuad, fsDisplay)
  val quadGeometry = TexturedQuadGeometry(gl)  

  val timeAtFirstFrame = Date().getTime()
  var timeAtLastFrame =  timeAtFirstFrame

  val camera = PerspectiveCamera(*Program.all)
  val quadrics = Array<Quadric>(6) { Quadric(it, *Program.all) }
  val lights = Array<Light>(3) { Light(it, *Program.all) }

  var frameWeight = 1.0f

  init {
    var index = 0
    quadrics[index].surface.set(Quadric.doubleplain)
            .translate(0.0f, -2.0f, 0.0f)
    quadrics[index].clipper.set(Quadric.doubleplain)
            .translate(0.0f, -1.0f, 0.0f)
    quadrics[index].color.set(0.0f, 0.5f, 0.25f, 1.0f)
    quadrics[index].reflectance.set(0.5f, 0.5f, 0.5f, 1.0f)
    index++

    quadrics[index].surface.set(Quadric.cone)
            .rotate(0.5f, 0.7f, 0.7f, 0.0f)
            .translate(2.0f, 2.0f, 0.0f)
    quadrics[index].clipper.set(Quadric.doubleplain)
            .scale(1.0f, 1.5f, 1.0f)
            .translate(0.0f, 0.5f, 0.0f)
    quadrics[index].color.set(0.8f, 0.8f, 0.1f, 1.0f)
    quadrics[index].reflectance.set(0.9f, 0.5f, 0.5f, 1.0f)
    index++

    quadrics[index].surface.set(Quadric.paraboloid)
            .rotate(kotlin.math.PI.toFloat(), 1.0f, 0.0f, 0.0f)
            .translate(-4.0f, 2.0f, -4.0f)
    quadrics[index].clipper.set(Quadric.doubleplain)
            .scale(1.0f, 1.5f, 1.0f)
            .translate(0.0f, 0.5f, 0.0f)
    quadrics[index].color.set(0.8f, 0.1f, 0.3f, 1.0f)
    quadrics[index].reflectance.set(0.5f, 0.5f, 0.5f, 1.0f)
    index++

    quadrics[index].surface.set(Quadric.paraboloid)
            .scale(1.0f, 0.5f, 1.0f)
            .rotate(kotlin.math.PI.toFloat(), 1.0f, 0.0f, 0.0f)
            .translate(-3.0f, 2.0f, 2.0f)
    quadrics[index].clipper.set(Quadric.doubleplain)
            .scale(1.0f, 1.5f, 1.0f)
            .translate(0.0f, 0.5f, 0.0f)
    quadrics[index].color.set(0.8f, 0.1f, 0.3f, 1.0f)
    quadrics[index].reflectance.set(0.5f, 0.5f, 0.5f, 1.0f)
    index++

    quadrics[index].surface.set(Quadric.sphere)
            .scale(2.0f, 1.0f, 1.2f)
            .translate(2.0f, 1.0f, 4.0f)
    quadrics[index].clipper.set(Quadric.doubleplain)
            .scale(1.0f, 1.5f, 1.0f)
            .translate(0.0f, 0.5f, 0.0f)
    quadrics[index].color.set(0.8f, 0.1f, 0.3f, 1.0f)
    quadrics[index].reflectance.set(0.5f, 0.5f, 0.5f, 1.0f)
    index++

    quadrics[index].surface.set(Quadric.sphere)
            .scale(1.0f, 2.0f, 2.0f)
            .rotate(kotlin.math.PI.toFloat()/4.0f,
                    0.2f, -1.0f, 0.0f)
            .translate(-1.0f, 1.0f, -6.0f)
    quadrics[index].clipper.set(Quadric.doubleplain)
            .scale(1.0f, 2.0f, 1.0f)
            .translate(0.0f, 1.0f, 0.0f)
    quadrics[index].color.set(0.8f, 0.1f, 0.3f, 1.0f)
    quadrics[index].reflectance.set(0.5f, 0.5f, 0.5f, 1.0f)
    index++

    index = 0
    lights[index].position.set(Vec4(-0.1f, 1.0f, 0.3f, 0.0f).normalize())
    lights[index].powerDensity.set(Vec3(0.9f, 0.9f, 0.8f) * 0.4f)
    index++

    lights[index].position.set(Vec4(-5.0f, 4.0f, 4.0f, 1.0f))
    lights[index].powerDensity.set(Vec3(0.9f, 0.9f, 0.5f) * 80.0f)
    index++

    lights[index].position.set(Vec4(5.0f, 2.5f, -1.0f, 1.0f))
    lights[index].powerDensity.set(Vec3(0.2f, 0.9f, 0.5f) * 54.0f)
    index++

    addComponentsAndGatherUniforms(*Program.all)
  }

  lateinit var defaultFramebuffer : DefaultFramebuffer
  lateinit var framebuffers : Pair<Framebuffer, Framebuffer>

  fun resize(gl : WebGL2RenderingContext, canvas : HTMLCanvasElement) {
    gl.viewport(0, 0, canvas.width, canvas.height)
    camera.setAspectRatio(canvas.width.toFloat() / canvas.height.toFloat())
    defaultFramebuffer = DefaultFramebuffer(canvas.width, canvas.height)
    framebuffers = (
            Framebuffer(gl, 1, canvas.width, canvas.height, GL.RGBA32F, GL.RGBA, GL.FLOAT)
            to
            Framebuffer(gl, 1, canvas.width, canvas.height, GL.RGBA32F, GL.RGBA, GL.FLOAT)
            )
  }

  @Suppress("UNUSED_PARAMETER")
  fun update(gl : WebGL2RenderingContext, keysPressed : Set<String>) {

    val timeAtThisFrame = Date().getTime() 
    val dt = (timeAtThisFrame - timeAtLastFrame).toFloat() / 1000.0f
    val t  = (timeAtThisFrame - timeAtFirstFrame).toFloat() / 1000.0f    
    timeAtLastFrame = timeAtThisFrame
    
    val moved = camera.move(dt, keysPressed)
    if (moved) {
      frameWeight = 1.0f
    } else {
      frameWeight = 1.0f / (1.0f / frameWeight + 1.0f)
    }

    framebuffers.first.bind(gl)
    traceProgram["previousFrame"]?.set( framebuffers.second.targets[0] )
    traceProgram["weight"]?.set( frameWeight )

    // clear the screen
    gl.clearColor(0.7f, 0.0f, 0.3f, 1.0f)
    gl.clearDepth(1.0f)
    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)

    traceProgram.draw(this, camera, *quadrics, *lights)
    quadGeometry.draw()


    defaultFramebuffer.bind(gl)
    displayProgram["frame"]?.set( framebuffers.second.targets[0] )

    // clear the screen
    gl.clearColor(0.7f, 0.0f, 0.3f, 1.0f)
    gl.clearDepth(1.0f)
    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)

    traceProgram.draw(this, camera, *quadrics, *lights)
    quadGeometry.draw()

    framebuffers = framebuffers.second to framebuffers.first
  }
}
