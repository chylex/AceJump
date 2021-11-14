package org.acejump.vim

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.change.change.ChangeVisualAction
import com.maddyhome.idea.vim.action.change.delete.DeleteVisualAction
import com.maddyhome.idea.vim.action.copy.YankVisualAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.vimSelectionStart
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.ij
import org.acejump.action.AceTagAction
import org.acejump.modes.JumpMode
import org.acejump.search.Tag
import org.acejump.session.SessionManager
import org.acejump.session.SessionState

class AceVimHandler(private val mode: AceVimMode) : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext) {
    val ij = editor.ij
    val caret = ij.caretModel.currentCaret
    val initialOffset = caret.offset
    val selectionStart = if (ij.inVisualMode) caret.vimSelectionStart else null
    
    val session = SessionManager.start(ij, mode.getJumpEditors(ij))
    
    session.defaultBoundary = mode.boundaries
    session.startJumpMode {
      object : JumpMode() {
        override fun accept(state: SessionState, acceptedTag: Tag): Boolean {
          state.act(AceTagAction.JumpToSearchStart, acceptedTag, wasUpperCase, isFinal = true)
          
          if (selectionStart != null) {
            caret.vimSetSelection(selectionStart, caret.offset, moveCaretToSelectionEnd = true)
          }
          else {
            val commandState = editor.vimStateMachine
            if (commandState.isOperatorPending) {
              val key = commandState.commandBuilder.keys.singleOrNull()?.keyChar
              
              commandState.reset()
              KeyHandler.getInstance().fullReset(editor)
              
              VimPlugin.getVisualMotion().enterVisualMode(editor, VimStateMachine.SubMode.VISUAL_CHARACTER)
              caret.vimSetSelection(caret.offset, initialOffset, moveCaretToSelectionEnd = true)
              
              val action = when (key) {
                'd' -> DeleteVisualAction()
                'c' -> ChangeVisualAction()
                'y' -> YankVisualAction()
                else -> null
              }
              
              if (action != null) {
                ApplicationManager.getApplication().invokeLater {
                  WriteAction.run<Nothing> {
                    commandState.commandBuilder.pushCommandPart(action)
                    
                    val cmd = commandState.commandBuilder.buildCommand()
                    val operatorArguments = OperatorArguments(
                      commandState.mappingState.mappingMode == MappingMode.OP_PENDING,
                      cmd.rawCount, commandState.mode, commandState.subMode
                    )
                    
                    commandState.setExecutingCommand(cmd)
                    injector.actionExecutor.executeVimAction(editor, action, context, operatorArguments)
                    // TODO does not update status
                  }
                }
              }
            }
          }
          
          mode.finishSession(ij, session)
          return true
        }
      }
    }
    
    mode.setupSession(ij, session)
  }
}
