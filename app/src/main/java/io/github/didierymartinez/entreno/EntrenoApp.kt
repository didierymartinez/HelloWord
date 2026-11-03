package io.github.didierymartinez.entreno

import android.app.Application
import android.content.Context
import io.github.didierymartinez.entreno.data.Repository
import io.github.didierymartinez.entreno.session.HealthConnect
import io.github.didierymartinez.entreno.session.Notifier
import io.github.didierymartinez.entreno.session.SessionManager

/** Dependencias compartidas de la app. */
object Graph {
    lateinit var repo: Repository
        private set
    lateinit var notifier: Notifier
        private set
    lateinit var session: SessionManager
        private set
    lateinit var health: HealthConnect
        private set

    fun init(ctx: Context) {
        repo = Repository(ctx.filesDir)
        notifier = Notifier(ctx).also { it.createChannels() }
        session = SessionManager(ctx, repo, notifier)
        health = HealthConnect(ctx)
    }
}

class EntrenoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
