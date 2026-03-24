package com.autexel.app.utils

/**
 * UndoRedoManager — generic undo/redo stack for table editing.
 * Stores snapshots of the table state. Max 30 states to keep memory bounded.
 */
class UndoRedoManager<T>(private val maxStates: Int = 30) {

    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    fun push(state: T) {
        undoStack.addLast(state)
        if (undoStack.size > maxStates) undoStack.removeFirst()
        redoStack.clear()   // New action clears redo history
    }

    fun undo(currentState: T): T? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(currentState)
        return undoStack.removeLast()
    }

    fun redo(currentState: T): T? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(currentState)
        return redoStack.removeLast()
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()

    fun clear() { undoStack.clear(); redoStack.clear() }

    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size
}
