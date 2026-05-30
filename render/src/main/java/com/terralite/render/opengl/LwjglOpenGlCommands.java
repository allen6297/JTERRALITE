package com.terralite.render.opengl;

import com.terralite.render.ClearColor;
import com.terralite.render.Viewport;
import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.mesh.DebugVertex;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LwjglOpenGlCommands implements OpenGlCommands {
    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 position;
            layout (location = 1) in vec3 color;
            uniform mat4 mvp;
            out vec3 vertexColor;

            void main() {
                vertexColor = color;
                gl_Position = mvp * vec4(position, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec3 vertexColor;
            out vec4 fragmentColor;

            void main() {
                fragmentColor = vec4(vertexColor, 1.0);
            }
            """;

    /** Floats per vertex: x, y, z, r, g, b */
    private static final int FLOATS_PER_VERTEX = 6;

    private int shaderProgram;
    private int mvpLocation = -1;
    private int nextMeshHandle = 1;
    private final Map<Integer, MeshResources> meshes = new HashMap<>();

    @Override
    public void createCapabilities() {
        GL.createCapabilities();
    }

    @Override
    public int createMesh(DebugMesh mesh) {
        Objects.requireNonNull(mesh, "mesh");
        if (shaderProgram == 0) {
            shaderProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            mvpLocation = GL20.glGetUniformLocation(shaderProgram, "mvp");
        }

        int vao = GL30.glGenVertexArrays();
        int vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, toFloatArray(mesh), GL15.GL_STATIC_DRAW);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        // location 0: position (vec3)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
        GL20.glEnableVertexAttribArray(0);
        // location 1: color (vec3)
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        int handle = nextMeshHandle++;
        meshes.put(handle, new MeshResources(vao, vbo, mesh.vertices().size()));
        return handle;
    }

    @Override
    public void viewport(Viewport viewport) {
        GL11.glViewport(0, 0, viewport.width(), viewport.height());
    }

    @Override
    public void clear(ClearColor clearColor) {
        GL11.glClearColor(clearColor.red(), clearColor.green(), clearColor.blue(), clearColor.alpha());
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void drawMesh(int meshHandle, float[] mvp) {
        Objects.requireNonNull(mvp, "mvp");
        if (mvp.length != 16) {
            throw new IllegalArgumentException("MVP matrix must be float[16], got float[" + mvp.length + "]");
        }
        MeshResources mesh = requireMesh(meshHandle);
        GL20.glUseProgram(shaderProgram);
        GL20.glUniformMatrix4fv(mvpLocation, false, mvp);
        GL30.glBindVertexArray(mesh.vao());
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, mesh.vertexCount());
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
    }

    @Override
    public void destroyMesh(int meshHandle) {
        MeshResources mesh = meshes.remove(meshHandle);
        if (mesh == null) {
            return;
        }

        GL15.glDeleteBuffers(mesh.vbo());
        GL30.glDeleteVertexArrays(mesh.vao());
        if (meshes.isEmpty() && shaderProgram != 0) {
            GL20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            mvpLocation = -1;
        }
    }

    private static int createProgram(String vertexShaderSource, String fragmentShaderSource) {
        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentShaderSource);
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            throw new IllegalStateException("Failed to link shader program: " + log);
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        return program;
    }

    private static int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Failed to compile shader: " + log);
        }

        return shader;
    }

    private MeshResources requireMesh(int meshHandle) {
        MeshResources mesh = meshes.get(meshHandle);
        if (mesh == null) {
            throw new IllegalArgumentException("Missing mesh handle: " + meshHandle);
        }
        return mesh;
    }

    private static float[] toFloatArray(DebugMesh mesh) {
        float[] values = new float[mesh.vertices().size() * FLOATS_PER_VERTEX];
        int index = 0;
        for (DebugVertex vertex : mesh.vertices()) {
            values[index++] = vertex.x();
            values[index++] = vertex.y();
            values[index++] = vertex.z();
            values[index++] = vertex.red();
            values[index++] = vertex.green();
            values[index++] = vertex.blue();
        }
        return values;
    }

    private record MeshResources(int vao, int vbo, int vertexCount) {
    }
}
