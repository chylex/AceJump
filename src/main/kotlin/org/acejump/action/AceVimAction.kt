package org.acejump.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.DumbAwareAction
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.action.change.change.ChangeVisualAction
import com.maddyhome.idea.vim.action.change.delete.DeleteVisualAction
import com.maddyhome.idea.vim.action.copy.YankVisualAction
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.vimSelectionStart
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.acejump.boundaries.StandardBoundaries.AFTER_CARET
import org.acejump.boundaries.StandardBoundaries.BEFORE_CARET
import org.acejump.boundaries.StandardBoundaries.CARET_LINE
import org.acejump.boundaries.StandardBoundaries.VISIBLE_ON_SCREEN
import org.acejump.modes.JumpMode
import org.acejump.search.Pattern
import org.acejump.search.Tag
import org.acejump.session.SessionManager
import org.acejump.session.SessionState

sealed class AceVimAction : DumbAwareAction() {
  protected abstract val mode: AceVimMode
  
  final override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val context = e.dataContext.vim
    
    val caret = editor.caretModel.currentCaret
    val initialOffset = caret.offset
    val selectionStart = if (editor.inVisualMode) caret.vimSelectionStart else null
    
    val session = SessionManager.start(editor, mode.getJumpEditors(editor))
    
    session.defaultBoundary = mode.boundaries
    session.startJumpMode {
      object : JumpMode() {
        override fun accept(state: SessionState, acceptedTag: Tag) {
          AceTagAction.JumpToSearchStart.invoke(acceptedTag, shiftMode = wasUpperCase, isFinal = true)
          
          if (selectionStart != null) {
            caret.vim.vimSetSelection(selectionStart, caret.offset, moveCaretToSelectionEnd = true)
          }
          else {
            val vim = editor.vim
            val keyHandler = KeyHandler.getInstance()
            if (keyHandler.isOperatorPending(vim.mode, keyHandler.keyHandlerState)) {
              val key = keyHandler.keyHandlerState.commandBuilder.keys.singleOrNull()?.keyChar
              
              keyHandler.fullReset(vim)
              
              AceVimUtil.enterVisualMode(vim, SelectionType.CHARACTER_WISE)
              caret.vim.vimSetSelection(caret.offset, initialOffset, moveCaretToSelectionEnd = true)
              
              val action = when (key) {
                'd'  -> DeleteVisualAction()
                'c'  -> ChangeVisualAction()
                'y'  -> YankVisualAction()
                else -> null
              }
              
              if (action != null) {
                ApplicationManager.getApplication().invokeLater {
                  WriteAction.run<Nothing> {
                    keyHandler.keyHandlerState.commandBuilder.pushCommandPart(action)
                    
                    val cmd = keyHandler.keyHandlerState.commandBuilder.buildCommand()
                    val operatorArguments = OperatorArguments(vim.mode is Mode.OP_PENDING, cmd.rawCount, injector.vimState.mode)
                    
                    injector.vimState.executingCommand = cmd
                    injector.actionExecutor.executeVimAction(vim, action, context, operatorArguments)
                  }
                  
                  keyHandler.reset(vim)
                }
              }
            }
          }
          
          injector.scroll.scrollCaretIntoView(editor.vim)
          mode.finishSession(editor, session)
        }
      }
    }
    
    mode.setupSession(editor, session)
  }
  
  class JumpAllEditors : AceVimAction() {
    override val mode = AceVimMode.JumpAllEditors
  }
  
  class JumpForward : AceVimAction() {
    override val mode = AceVimMode.Jump(AFTER_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpBackward : AceVimAction() {
    override val mode = AceVimMode.Jump(BEFORE_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpTillForward : AceVimAction() {
    override val mode = AceVimMode.JumpTillForward(AFTER_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpTillBackward : AceVimAction() {
    override val mode = AceVimMode.JumpTillBackward(BEFORE_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpOnLineForward : AceVimAction() {
    override val mode = AceVimMode.Jump(AFTER_CARET.intersection(CARET_LINE))
  }
  
  class JumpOnLineBackward : AceVimAction() {
    override val mode = AceVimMode.Jump(BEFORE_CARET.intersection(CARET_LINE))
  }
  
  class JumpLineIndentsForward : AceVimAction() {
    override val mode = AceVimMode.JumpToPattern(Pattern.LINE_INDENTS, AFTER_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpLineIndentsBackward : AceVimAction() {
    override val mode = AceVimMode.JumpToPattern(Pattern.LINE_INDENTS, BEFORE_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpLWordForward : AceVimAction() {
    override val mode = AceVimMode.JumpToPattern(Pattern.VIM_LWORD, AFTER_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpUWordForward : AceVimAction() {
    override val mode = AceVimMode.JumpToPattern(Pattern.VIM_UWORD, AFTER_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpLWordBackward : AceVimAction() {
    override val mode = AceVimMode.JumpToPattern(Pattern.VIM_LWORD, BEFORE_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpUWordBackward : AceVimAction() {
    override val mode = AceVimMode.JumpToPattern(Pattern.VIM_UWORD, BEFORE_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpLWordEndForward : AceVimAction() {
    override val mode = AceVimMode.JumpToPattern(Pattern.VIM_LWORD_END, AFTER_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpUWordEndForward : AceVimAction() {
    override val mode = AceVimMode.JumpToPattern(Pattern.VIM_UWORD_END, AFTER_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpLWordEndBackward : AceVimAction() {
    override val mode = AceVimMode.JumpToPattern(Pattern.VIM_LWORD_END, BEFORE_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpUWordEndBackward : AceVimAction() {
    override val mode = AceVimMode.JumpToPattern(Pattern.VIM_UWORD_END, BEFORE_CARET.intersection(VISIBLE_ON_SCREEN))
  }
  
  class JumpAllEditorsGoToDeclaration : DumbAwareAction() {
    override fun update(action: AnActionEvent) {
      action.presentation.isEnabled = action.getData(CommonDataKeys.EDITOR) != null
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
      return ActionUpdateThread.BGT
    }
    
    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: return
      val session = SessionManager.start(editor, AceVimMode.JumpAllEditors.getJumpEditors(editor))
      
      session.defaultBoundary = VISIBLE_ON_SCREEN
      session.startJumpMode {
        object : JumpMode() {
          override fun accept(state: SessionState, acceptedTag: Tag) {
            AceTagAction.GoToDeclaration.invoke(acceptedTag, shiftMode = wasUpperCase, isFinal = true)
          }
        }
      }
    }
  }
}
