package com.practicetracker.data.db.dao

import androidx.room.*
import com.practicetracker.data.db.entity.SkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity)

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Delete
    suspend fun deleteSkill(skill: SkillEntity)

    @Query("SELECT * FROM skills ORDER BY label ASC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE id = :id")
    suspend fun getSkillById(id: String): SkillEntity?

    @Query("SELECT * FROM skills WHERE label LIKE '%' || :query || '%' ORDER BY label ASC")
    fun searchSkills(query: String): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE lower(label) = lower(:label) LIMIT 1")
    suspend fun findByLabelIgnoreCase(label: String): SkillEntity?

    @Query("""
        SELECT s.*, COUNT(ps.pieceId) as usageCount
        FROM skills s
        LEFT JOIN piece_skills ps ON s.id = ps.skillId
        GROUP BY s.id
        ORDER BY s.label ASC
    """)
    fun getSkillsWithUsageCount(): Flow<List<SkillWithUsageCount>>

    @Query("SELECT COUNT(*) FROM piece_skills WHERE skillId = :skillId")
    suspend fun getPieceCountForSkill(skillId: String): Int
}

data class SkillWithUsageCount(
    val id: String,
    val label: String,
    val scope: String,
    val createdAt: java.time.Instant,
    val usageCount: Int
)
