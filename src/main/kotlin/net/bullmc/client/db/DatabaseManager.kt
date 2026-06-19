package net.bullmc.client.db

import net.bullmc.client.core.util.LauncherPaths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp

object DatabaseManager {
    private var connection: Connection? = null

    fun init() {
        LauncherPaths.init()
        val dbFile = LauncherPaths.getDbFile()
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        createTables()
        println("[DB] Инициализирована: ${dbFile.absolutePath}")
    }

    private fun createTables() {
        execUpdate("""
            CREATE TABLE IF NOT EXISTS players (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nick TEXT UNIQUE NOT NULL,
                license TEXT,
                first_launch TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_launch TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                total_launches INTEGER DEFAULT 0,
                total_play_time_ms INTEGER DEFAULT 0
            )
        """)

        execUpdate("""
            CREATE TABLE IF NOT EXISTS launch_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nick TEXT NOT NULL,
                version TEXT NOT NULL,
                server_ip TEXT,
                started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                ended_at TIMESTAMP,
                exit_code INTEGER,
                duration_ms INTEGER DEFAULT 0,
                crash_log TEXT
            )
        """)

        execUpdate("""
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """)
    }

    fun savePlayer(nick: String, license: String? = null) {
        val sql = """
            INSERT INTO players (nick, license, last_launch, total_launches)
            VALUES (?, ?, CURRENT_TIMESTAMP, 1)
            ON CONFLICT(nick) DO UPDATE SET
                license = COALESCE(excluded.license, players.license),
                last_launch = CURRENT_TIMESTAMP,
                total_launches = total_launches + 1
        """
        execUpdate(sql, nick, license)
    }

    fun updatePlayTime(nick: String, durationMs: Long) {
        execUpdate(
            "UPDATE players SET total_play_time_ms = total_play_time_ms + ? WHERE nick = ?",
            durationMs, nick
        )
    }

    fun getPlayers(): List<PlayerRecord> {
        val rs = execQuery("SELECT * FROM players ORDER BY last_launch DESC")
        val players = mutableListOf<PlayerRecord>()
        while (rs.next()) {
            players.add(
                PlayerRecord(
                    nick = rs.getString("nick"),
                    license = rs.getString("license"),
                    firstLaunch = rs.getTimestamp("first_launch")?.time ?: 0,
                    lastLaunch = rs.getTimestamp("last_launch")?.time ?: 0,
                    totalLaunches = rs.getInt("total_launches"),
                    totalPlayTimeMs = rs.getLong("total_play_time_ms")
                )
            )
        }
        return players
    }

    fun startLaunch(nick: String, version: String, serverIp: String? = null): Long {
        val sql = "INSERT INTO launch_history (nick, version, server_ip, started_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)"
        execUpdate(sql, nick, version, serverIp)
        val rs = execQuery("SELECT last_insert_rowid()")
        return if (rs.next()) rs.getLong(1) else -1
    }

    fun endLaunch(id: Long, exitCode: Int, durationMs: Long, crashLog: String? = null) {
        execUpdate(
            "UPDATE launch_history SET ended_at = CURRENT_TIMESTAMP, exit_code = ?, duration_ms = ?, crash_log = ? WHERE id = ?",
            exitCode, durationMs, crashLog, id
        )
    }

    fun getLaunchHistory(nick: String? = null, limit: Int = 50): List<LaunchRecord> {
        val sql = if (nick != null) {
            "SELECT * FROM launch_history WHERE nick = ? ORDER BY started_at DESC LIMIT ?"
        } else {
            "SELECT * FROM launch_history ORDER BY started_at DESC LIMIT ?"
        }
        val rs = if (nick != null) {
            execQuery(sql, nick, limit)
        } else {
            execQuery(sql, limit)
        }
        val records = mutableListOf<LaunchRecord>()
        while (rs.next()) {
            records.add(
                LaunchRecord(
                    id = rs.getLong("id"),
                    nick = rs.getString("nick"),
                    version = rs.getString("version"),
                    serverIp = rs.getString("server_ip"),
                    startedAt = rs.getTimestamp("started_at")?.time ?: 0,
                    endedAt = rs.getTimestamp("ended_at")?.time,
                    exitCode = rs.getObject("exit_code") as? Int,
                    durationMs = rs.getLong("duration_ms"),
                    crashLog = rs.getString("crash_log")
                )
            )
        }
        return records
    }

    fun saveSetting(key: String, value: String) {
        execUpdate(
            "INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value",
            key, value
        )
    }

    fun getSetting(key: String): String? {
        val rs = execQuery("SELECT value FROM settings WHERE key = ?", key)
        return if (rs.next()) rs.getString("value") else null
    }

    fun saveSetting(key: String, value: Int) = saveSetting(key, value.toString())

    fun getSettingInt(key: String, default: Int): Int = getSetting(key)?.toIntOrNull() ?: default

    private fun execUpdate(sql: String, vararg params: Any?) {
        val conn = connection ?: return
        val stmt = conn.prepareStatement(sql)
        params.forEachIndexed { i, param ->
            when (param) {
                is String -> stmt.setString(i + 1, param)
                is Int -> stmt.setInt(i + 1, param)
                is Long -> stmt.setLong(i + 1, param)
                is Timestamp -> stmt.setTimestamp(i + 1, param)
                null -> stmt.setNull(i + 1, java.sql.Types.NULL)
            }
        }
        stmt.executeUpdate()
        stmt.close()
    }

    private fun execQuery(sql: String, vararg params: Any?): ResultSet {
        val conn = connection ?: throw IllegalStateException("БД не инициализирована")
        val stmt = conn.prepareStatement(sql)
        params.forEachIndexed { i, param ->
            when (param) {
                is String -> stmt.setString(i + 1, param)
                is Int -> stmt.setInt(i + 1, param)
                is Long -> stmt.setLong(i + 1, param)
                null -> stmt.setNull(i + 1, java.sql.Types.NULL)
            }
        }
        return stmt.executeQuery()
    }

    fun close() {
        connection?.close()
        connection = null
    }
}

data class PlayerRecord(
    val nick: String,
    val license: String?,
    val firstLaunch: Long,
    val lastLaunch: Long,
    val totalLaunches: Int,
    val totalPlayTimeMs: Long
) {
    val formattedPlayTime: String
        get() {
            val hours = totalPlayTimeMs / 3600000
            val minutes = (totalPlayTimeMs % 3600000) / 60000
            return "${hours}ч ${minutes}м"
        }
}

data class LaunchRecord(
    val id: Long,
    val nick: String,
    val version: String,
    val serverIp: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val exitCode: Int?,
    val durationMs: Long,
    val crashLog: String?
)
