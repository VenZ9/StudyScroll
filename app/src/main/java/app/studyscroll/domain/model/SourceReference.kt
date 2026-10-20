package app.studyscroll.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SourceReference(
    val pageNumber: Int,
    val excerpt: String
) {
    init {
        require(pageNumber >= 1) { "Page number must be one-based (>= 1), got $pageNumber" }
    }
}
