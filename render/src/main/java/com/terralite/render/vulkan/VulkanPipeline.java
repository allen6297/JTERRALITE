package com.terralite.render.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.util.shaderc.Shaderc;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Holds the Vulkan pipeline layout and graphics pipeline.
 * Compiles GLSL vertex/fragment shaders to SPIRV via shaderc at creation time.
 * Uses a push constant for the 4x4 MVP matrix (64 bytes).
 */
public final class VulkanPipeline {
    /** Size in bytes of the MVP push constant (mat4 = 16 floats). */
    public static final int MVP_PUSH_CONSTANT_SIZE = 16 * Float.BYTES;

    public final long pipelineLayout;
    public final long pipeline;

    private VulkanPipeline(long pipelineLayout, long pipeline) {
        this.pipelineLayout = pipelineLayout;
        this.pipeline = pipeline;
    }

    public static VulkanPipeline create(VulkanContext ctx, VulkanSwapchain swapchain) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertSpirv = compileSpirvFromGlsl(VERTEX_SHADER_GLSL, Shaderc.shaderc_glsl_vertex_shader, "chunk.vert");
            ByteBuffer fragSpirv = compileSpirvFromGlsl(FRAGMENT_SHADER_GLSL, Shaderc.shaderc_glsl_fragment_shader, "chunk.frag");

            long vertModule = createShaderModule(ctx.device, vertSpirv, stack);
            long fragModule = createShaderModule(ctx.device, fragSpirv, stack);

            long pipelineLayout = createPipelineLayout(ctx.device, stack);
            long pipeline = createPipeline(ctx.device, swapchain, pipelineLayout, vertModule, fragModule, stack);

            vkDestroyShaderModule(ctx.device, vertModule, null);
            vkDestroyShaderModule(ctx.device, fragModule, null);

            return new VulkanPipeline(pipelineLayout, pipeline);
        }
    }

    public void destroy(VulkanContext ctx) {
        vkDestroyPipeline(ctx.device, pipeline, null);
        vkDestroyPipelineLayout(ctx.device, pipelineLayout, null);
    }

    // ---- shaders ----

    private static final String VERTEX_SHADER_GLSL = """
            #version 450
            layout(location = 0) in vec3 inPosition;
            layout(location = 1) in vec3 inColor;
            layout(location = 0) out vec3 fragColor;
            layout(push_constant) uniform PushConstants {
                mat4 mvp;
            } push;
            void main() {
                gl_Position = push.mvp * vec4(inPosition, 1.0);
                fragColor = inColor;
            }
            """;

    private static final String FRAGMENT_SHADER_GLSL = """
            #version 450
            layout(location = 0) in vec3 fragColor;
            layout(location = 0) out vec4 outColor;
            void main() {
                outColor = vec4(fragColor, 1.0);
            }
            """;

    private static ByteBuffer compileSpirvFromGlsl(String source, int shaderKind, String filename) {
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        try {
            long result = Shaderc.shaderc_compile_into_spv(compiler, source, shaderKind, filename, "main", options);
            if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
                String error = Shaderc.shaderc_result_get_error_message(result);
                Shaderc.shaderc_result_release(result);
                throw new IllegalStateException("Shader compilation failed (" + filename + "): " + error);
            }
            ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
            if (spirv == null) throw new IllegalStateException("Empty SPIRV output for " + filename);
            // Copy to a standalone buffer since result will be freed
            ByteBuffer copy = ByteBuffer.allocateDirect(spirv.remaining());
            copy.put(spirv).flip();
            Shaderc.shaderc_result_release(result);
            return copy;
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }

    private static long createShaderModule(VkDevice device, ByteBuffer spirv, MemoryStack stack) {
        VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(spirv);

        LongBuffer modulePtr = stack.mallocLong(1);
        VulkanUtils.check(vkCreateShaderModule(device, createInfo, null, modulePtr), "Failed to create shader module");
        return modulePtr.get(0);
    }

    private static long createPipelineLayout(VkDevice device, MemoryStack stack) {
        VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .offset(0)
                .size(MVP_PUSH_CONSTANT_SIZE);

        VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pPushConstantRanges(pushConstantRange);

        LongBuffer layoutPtr = stack.mallocLong(1);
        VulkanUtils.check(vkCreatePipelineLayout(device, layoutInfo, null, layoutPtr), "Failed to create pipeline layout");
        return layoutPtr.get(0);
    }

    private static long createPipeline(VkDevice device, VulkanSwapchain swapchain,
                                        long pipelineLayout, long vertModule, long fragModule, MemoryStack stack) {
        // Shader stages
        VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
        stages.get(0).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_VERTEX_BIT).module(vertModule).pName(stack.UTF8("main"));
        stages.get(1).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_FRAGMENT_BIT).module(fragModule).pName(stack.UTF8("main"));

        // Vertex input: binding 0, stride = 6 floats (xyz rgb)
        int stride = 6 * Float.BYTES;
        VkVertexInputBindingDescription.Buffer bindingDesc = VkVertexInputBindingDescription.calloc(1, stack)
                .binding(0).stride(stride).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        VkVertexInputAttributeDescription.Buffer attrDesc = VkVertexInputAttributeDescription.calloc(2, stack);
        attrDesc.get(0).binding(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT).offset(0);
        attrDesc.get(1).binding(0).location(1).format(VK_FORMAT_R32G32B32_SFLOAT).offset(3 * Float.BYTES);

        VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(bindingDesc)
                .pVertexAttributeDescriptions(attrDesc);

        VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .primitiveRestartEnable(false);

        // Dynamic viewport + scissor
        VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1).scissorCount(1);

        VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .cullMode(VK_CULL_MODE_BACK_BIT)
                .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                .lineWidth(1.0f);

        VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                        VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                .blendEnable(false);

        VkPipelineColorBlendStateCreateInfo colorBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(false)
                .pAttachments(colorBlendAttachment);

        VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                .depthTestEnable(true)
                .depthWriteEnable(true)
                .depthCompareOp(VK_COMPARE_OP_LESS)
                .depthBoundsTestEnable(false)
                .stencilTestEnable(false);

        // Dynamic states: viewport and scissor set each frame
        VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

        VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .pStages(stages)
                .pVertexInputState(vertexInput)
                .pInputAssemblyState(inputAssembly)
                .pViewportState(viewportState)
                .pRasterizationState(rasterizer)
                .pMultisampleState(multisampling)
                .pColorBlendState(colorBlend)
                .pDepthStencilState(depthStencil)
                .pDynamicState(dynamicState)
                .layout(pipelineLayout)
                .renderPass(swapchain.renderPass)
                .subpass(0);

        LongBuffer pipelinePtr = stack.mallocLong(1);
        VulkanUtils.check(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pipelinePtr),
                "Failed to create graphics pipeline");
        return pipelinePtr.get(0);
    }
}
