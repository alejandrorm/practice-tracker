package com.practicetracker.data.db.dao

import androidx.room.*
import com.practicetracker.data.db.entity.PieceEntity
import com.practicetracker.data.db.entity.PieceSkillEntity
import com.practicetracker.data.db.relation.PieceWithSkills
import kotlinx.coroutines.flow.Flow

@Dao
interface PieceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPiece(piece: PieceEntity)

    @Update
    suspend fun updatePiece(piece: PieceEntity)

    @Delete
    suspend fun deletePiece(piece: PieceEntity)

    @Query("SELECT * FROM pieces ORDER BY title ASC")
    fun getAllPieces(): Flow<List<PieceEntity>>

    @Query("SELECT * FROM pieces WHERE id = :id")
    fun getPieceById(id: String): Flow<PieceEntity?>

    @Query("SELECT * FROM pieces WHERE title LIKE '%' || :query || '%' OR composer LIKE '%' || :query || '%' OR book LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchPieces(query: String): Flow<List<PieceEntity>>

    @Query("SELECT * FROM pieces WHERE type = :type ORDER BY title ASC")
    fun getPiecesByType(type: String): Flow<List<PieceEntity>>

    @Transaction
    @Query("SELECT * FROM pieces ORDER BY title ASC")
    fun getAllPiecesWithSkills(): Flow<List<PieceWithSkills>>

    @Transaction
    @Query("SELECT * FROM pieces WHERE id = :id")
    fun getPieceWithSkills(id: String): Flow<PieceWithSkills?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPieceSkill(pieceSkill: PieceSkillEntity)

    @Delete
    suspend fun deletePieceSkill(pieceSkill: PieceSkillEntity)

    @Query("DELETE FROM piece_skills WHERE pieceId = :pieceId AND skillId = :skillId")
    suspend fun removePieceSkill(pieceId: String, skillId: String)

    @Query("DELETE FROM piece_skills WHERE pieceId = :pieceId")
    suspend fun removeAllSkillsFromPiece(pieceId: String)

    @Query("SELECT * FROM piece_skills WHERE pieceId = :pieceId ORDER BY `order` ASC")
    suspend fun getPieceSkillsForPiece(pieceId: String): List<PieceSkillEntity>
}
