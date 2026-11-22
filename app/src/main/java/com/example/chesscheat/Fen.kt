package com.example.chesscheat

object Fen {
    fun fromBoard(
        board: Array<Array<Char?>>,
        sideToMove: Char = 'w',
        castling: String = "KQkq",
        enPassant: String = "-",
        halfmove: Int = 0,
        fullmove: Int = 1
    ): String {
        val sb = StringBuilder()
        for (r in 0 until 8) {
            var empty = 0
            for (c in 0 until 8) {
                val p = board[r][c]
                if (p == null) empty++
                else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    sb.append(p)
                }
            }
            if (empty > 0) sb.append(empty)
            if (r < 7) sb.append('/')
        }
        return "$sb $sideToMove $castling $enPassant $halfmove $fullmove"
    }
}
