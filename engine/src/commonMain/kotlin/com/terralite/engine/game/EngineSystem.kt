package com.terralite.engine.game

interface EngineSystem {
    fun initialize(context: EngineContext) {}
    fun start(context: EngineContext) {}
    fun stop(context: EngineContext) {}
}
