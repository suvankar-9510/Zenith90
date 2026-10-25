package com.zenith90.app.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromBlockCategory(value: BlockCategory): String = value.name

    @TypeConverter
    fun toBlockCategory(value: String): BlockCategory = BlockCategory.valueOf(value)

    @TypeConverter
    fun fromPriority(value: TaskPriority): String = value.name

    @TypeConverter
    fun toPriority(value: String): TaskPriority = TaskPriority.valueOf(value)

    @TypeConverter
    fun fromWorkoutType(value: WorkoutModality): String = value.name

    @TypeConverter
    fun toWorkoutType(value: String): WorkoutModality = WorkoutModality.valueOf(value)
}

enum class BlockCategory { WAKE_UP, STUDY, WORKOUT, ROUTINE }
enum class TaskPriority(val label: String, val weight: Int) {
    HIGH("P1 • CRITICAL", 3),
    MEDIUM("P2 • CORE", 2),
    LOW("P3 • AUXILIARY", 1)
}
enum class WorkoutModality(val displayName: String) {
    GYM("Gym Hypertrophy / Strength"),
    HOME_CALISTHENICS("Home Calisthenics / Bodyweight")
}

@Entity(
    tableName = "daily_logs",
    indices = [Index(value = ["userId", "dateEpochDay"], unique = true)]
)
data class DailyLogEntity(
    val userId: String = "guest",
    val dateEpochDay: Long,
    @PrimaryKey val logKey: String = "${userId}_${dateEpochDay}",
    val targetWakeTimeMinutes: Int = 330,
    val actualWakeTimeMinutes: Int? = null,
    val totalStudyMinutes: Int = 0,
    val targetStudyMinutes: Int = 360,
    val workoutModality: WorkoutModality = WorkoutModality.GYM,
    val workoutCompleted: Boolean = false,
    val plannedNightBeforeTimestamp: Long? = null,
    val reflectionNote: String = "",
    val completionRatio: Float = 0f
) {
    val wakeDeviationMinutes: Int?
        get() = actualWakeTimeMinutes?.let { it - targetWakeTimeMinutes }

    val isWakeGoalMet: Boolean
        get() = actualWakeTimeMinutes != null && actualWakeTimeMinutes <= (targetWakeTimeMinutes + 15)

    val isOverallStreakDay: Boolean
        get() = isWakeGoalMet && (totalStudyMinutes >= 240) && workoutCompleted

    companion object {
        fun createKey(userId: String, epochDay: Long): String = "${userId}_$epochDay"
    }
}

@Entity(
    tableName = "schedule_blocks",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["logKey"],
            childColumns = ["logKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("logKey"), Index("userId")]
)
data class ScheduleBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "guest",
    val dateEpochDay: Long = 0,
    val logKey: String = "${userId}_${dateEpochDay}",
    val startTimeMinutes: Int,
    val durationMinutes: Int,
    val title: String,
    val examSubjectTag: String? = null,
    val category: BlockCategory = BlockCategory.STUDY,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "workout_entries",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["logKey"],
            childColumns = ["logKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("logKey"), Index("userId")]
)
data class WorkoutEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "guest",
    val dateEpochDay: Long = 0,
    val logKey: String = "${userId}_${dateEpochDay}",
    val modality: WorkoutModality,
    val exerciseName: String,
    val targetSets: Int,
    val completedSets: Int = 0,
    val repsAndLoad: String,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "badges",
    indices = [Index(value = ["userId", "badgeId"], unique = true)]
)
data class BadgeEntity(
    val userId: String = "guest",
    val badgeId: String,
    @PrimaryKey val badgeKey: String = "${userId}_${badgeId}",
    val title: String,
    val subtitle: String,
    val criteriaDescription: String,
    val isUnlocked: Boolean = false,
    val unlockedEpochMillis: Long? = null,
    val progressCurrent: Int = 0,
    val progressTarget: Int = 1,
    val accentHexStart: Long,
    val accentHexEnd: Long
)

data class FullDaySnapshot(
    @Embedded val dailyLog: DailyLogEntity,
    @Relation(parentColumn = "logKey", entityColumn = "logKey")
    val scheduleBlocks: List<ScheduleBlockEntity>,
    @Relation(parentColumn = "logKey", entityColumn = "logKey")
    val workoutEntries: List<WorkoutEntryEntity>
)

@Dao
interface ZenithDao {
    @Transaction
    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND dateEpochDay = :epochDay")
    fun observeDaySnapshot(userId: String, epochDay: Long): Flow<FullDaySnapshot?>

    @Transaction
    @Query("SELECT * FROM daily_logs WHERE userId = :userId ORDER BY dateEpochDay DESC")
    fun observeAllDaySnapshotsDesc(userId: String): Flow<List<FullDaySnapshot>>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND dateEpochDay BETWEEN :startEpoch AND :endEpoch ORDER BY dateEpochDay ASC")
    fun observeLogsInRange(userId: String, startEpoch: Long, endEpoch: Long): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND dateEpochDay = :epochDay LIMIT 1")
    suspend fun getDailyLog(userId: String, epochDay: Long): DailyLogEntity?

    @Upsert
    suspend fun upsertDailyLog(log: DailyLogEntity)

    @Upsert
    suspend fun upsertScheduleBlock(block: ScheduleBlockEntity): Long

    @Delete
    suspend fun deleteScheduleBlock(block: ScheduleBlockEntity)

    @Query("UPDATE schedule_blocks SET dateEpochDay = :nextDayEpoch, logKey = :newLogKey, isCompleted = 0 WHERE id = :blockId")
    suspend fun rescheduleBlockToNextDay(blockId: Long, nextDayEpoch: Long, newLogKey: String)

    @Upsert
    suspend fun upsertWorkoutEntry(entry: WorkoutEntryEntity)

    @Delete
    suspend fun deleteWorkoutEntry(entry: WorkoutEntryEntity)

    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY isUnlocked DESC, progressTarget ASC")
    fun observeAllBadges(userId: String): Flow<List<BadgeEntity>>

    @Upsert
    suspend fun upsertBadges(badges: List<BadgeEntity>)
}

@Database(
    entities = [
        DailyLogEntity::class,
        ScheduleBlockEntity::class,
        WorkoutEntryEntity::class,
        BadgeEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ZenithDatabase : RoomDatabase() {
    abstract fun zenithDao(): ZenithDao

    companion object {
        @Volatile
        private var INSTANCE: ZenithDatabase? = null

        fun getDatabase(context: Context): ZenithDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ZenithDatabase::class.java,
                    "zenith90_pixel.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }

        fun initialBadgesForUser(userId: String): List<BadgeEntity> = listOf(
            BadgeEntity(
                badgeKey = "${userId}_5am_club",
                userId = userId,
                badgeId = "5am_club",
                title = "THE 5 AM CLUB",
                subtitle = "Circadian Mastery",
                criteriaDescription = "7 consecutive days hitting your target wake-up window.",
                progressTarget = 7,
                accentHexStart = 0xFFF6D365,
                accentHexEnd = 0xFFFDA085
            ),
            BadgeEntity(
                badgeKey = "${userId}_iron_and_ink",
                userId = userId,
                badgeId = "iron_and_ink",
                title = "IRON & INK",
                subtitle = "Dual Scholar-Athlete",
                criteriaDescription = "Complete a full physical training block and 6+ hours of deep study in a single day.",
                progressTarget = 1,
                accentHexStart = 0xFF84FAB0,
                accentHexEnd = 0xFF8FD3F4
            ),
            BadgeEntity(
                badgeKey = "${userId}_night_architect",
                userId = userId,
                badgeId = "night_architect",
                title = "NIGHT ARCHITECT",
                subtitle = "Nocturnal Precision",
                criteriaDescription = "Lock in your next-day blueprint before 10:30 PM for 7 consecutive nights.",
                progressTarget = 7,
                accentHexStart = 0xFFA18CD1,
                accentHexEnd = 0xFFFBC2EB
            ),
            BadgeEntity(
                badgeKey = "${userId}_relentless_30",
                userId = userId,
                badgeId = "relentless_30",
                title = "RELENTLESS 30",
                subtitle = "First Month Forged",
                criteriaDescription = "30 days of unbroken transformation logs leading to January.",
                progressTarget = 30,
                accentHexStart = 0xFFF6D365,
                accentHexEnd = 0xFFFDA085
            ),
            BadgeEntity(
                badgeKey = "${userId}_relentless_60",
                userId = userId,
                badgeId = "relentless_60",
                title = "RELENTLESS 60",
                subtitle = "Deep Momentum",
                criteriaDescription = "60 days of unbroken transformation logs leading to January.",
                progressTarget = 60,
                accentHexStart = 0xFF84FAB0,
                accentHexEnd = 0xFF8FD3F4
            ),
            BadgeEntity(
                badgeKey = "${userId}_relentless_90",
                userId = userId,
                badgeId = "relentless_90",
                title = "ZENITH 90",
                subtitle = "Apex Readiness",
                criteriaDescription = "90 days of unbroken discipline prior to the January examination.",
                progressTarget = 90,
                accentHexStart = 0xFFD4AF37,
                accentHexEnd = 0xFFF3E5AB
            )
        )
    }
}
