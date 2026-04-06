package com.practicetracker.data.repository

import com.practicetracker.data.db.dao.PieceDao
import com.practicetracker.data.db.dao.SkillDao
import com.practicetracker.data.db.entity.PieceSkillEntity
import com.practicetracker.data.mapper.toDomain
import com.practicetracker.data.mapper.toEntity
import com.practicetracker.domain.model.Piece
import com.practicetracker.domain.model.Skill
import com.practicetracker.domain.model.SkillScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PieceRepository @Inject constructor(
    private val pieceDao: PieceDao,
    private val skillDao: SkillDao
) {
    fun getAllPiecesWithSkills(): Flow<List<Piece>> =
        pieceDao.getAllPiecesWithSkills().map { list ->
            list.map { it.toDomain() }
        }

    fun getPieceWithSkills(id: String): Flow<Piece?> =
        pieceDao.getPieceWithSkills(id).map { it?.toDomain() }

    fun searchPieces(query: String): Flow<List<Piece>> =
        pieceDao.getAllPiecesWithSkills().map { list ->
            list.filter { pws ->
                pws.piece.title.contains(query, ignoreCase = true) ||
                pws.piece.composer?.contains(query, ignoreCase = true) == true ||
                pws.piece.book?.contains(query, ignoreCase = true) == true
            }.map { it.toDomain() }
        }

    suspend fun savePiece(piece: Piece) {
        pieceDao.insertPiece(piece.toEntity())
        pieceDao.removeAllSkillsFromPiece(piece.id)
        piece.skills.forEachIndexed { index, skill ->
            val resolvedSkill = getOrCreateSkill(skill.label, skill.scope)
            pieceDao.insertPieceSkill(PieceSkillEntity(piece.id, resolvedSkill.id, index))
        }
    }

    suspend fun deletePiece(piece: Piece) {
        pieceDao.deletePiece(piece.toEntity())
    }

    suspend fun addSkillToPiece(pieceId: String, skillLabel: String, scope: SkillScope): Skill {
        val skill = getOrCreateSkill(skillLabel, scope)
        val existingLinks = pieceDao.getPieceSkillsForPiece(pieceId)
        val nextOrder = (existingLinks.maxOfOrNull { it.order } ?: -1) + 1
        pieceDao.insertPieceSkill(PieceSkillEntity(pieceId, skill.id, nextOrder))
        return skill
    }

    suspend fun removeSkillFromPiece(pieceId: String, skillId: String) {
        pieceDao.removePieceSkill(pieceId, skillId)
    }

    fun searchSkillsInLibrary(query: String): Flow<List<Skill>> =
        skillDao.searchSkills(query).map { list -> list.map { it.toDomain() } }

    suspend fun getSkillById(id: String): Skill? = skillDao.getSkillById(id)?.toDomain()

    /** Returns an existing Skill matching the label (case-insensitive), or creates a new one. */
    suspend fun getOrCreateSkill(label: String, scope: SkillScope): Skill {
        val existing = skillDao.findByLabelIgnoreCase(label)
        if (existing != null) return existing.toDomain()
        val newSkill = Skill(
            id = UUID.randomUUID().toString(),
            label = label,
            scope = scope,
            createdAt = Instant.now()
        )
        skillDao.insertSkill(newSkill.toEntity())
        return newSkill
    }
}
