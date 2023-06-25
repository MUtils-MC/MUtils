package de.miraculixx.mchallenge.global

import de.miraculixx.mvanilla.messages.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.event.ClickEvent
import java.io.File
import java.io.InputStream

interface ResourcePackChallenge {
    val packMeta: String
        get() = "{\"pack\":{\"description\":\"MUtils-Challenge ResourcePack - AGPLv3 Licence\",\"pack_format\":13}}"
    val packIcon: InputStream?
        get() = javaClass.getResourceAsStream("/data/pack.png")
    val transparency: InputStream?
        get() = javaClass.getResourceAsStream("/data/transparent.png")

    fun createRPStructure(root: File): File {
        if (root.exists()) root.deleteRecursively()
        root.mkdirs()
        File(root, "pack.mcmeta").writeText(packMeta)
        packIcon?.readBytes()?.let { File(root, "pack.png").writeBytes(it) }
        val ressourceFolder = File(root, "assets/minecraft")
        ressourceFolder.mkdirs()
        File(ressourceFolder, "blockstates").mkdir()
        File(ressourceFolder, "textures").mkdir()
        File(ressourceFolder, "models").mkdir()
        return ressourceFolder
    }

    fun error(audience: Audience): Boolean {
        audience.sendMessage(prefix + cmp("MWeb is needed to play this Challenge! Please install it ", cError) + cmp("here", cError, underlined = true)
            .clickEvent(ClickEvent.openUrl("https://modrinth.com/mod/mweb"))
            .addHover(cmp("Click to open")))
        return false
    }
}