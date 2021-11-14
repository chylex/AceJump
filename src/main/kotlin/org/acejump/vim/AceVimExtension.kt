package org.acejump.vim

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.key.MappingOwner
import org.acejump.boundaries.StandardBoundaries
import org.acejump.search.Pattern

class AceVimExtension : VimExtension {
  private companion object {
    const val NAME = "AceJump"
    const val PLUG_PREFIX = "<Plug>(acejump)"
    
    private val OWNER = MappingOwner.Plugin.get(NAME)
    
    private fun register(keys: String, mode: AceVimMode) {
      val keysPrefixed = injector.parser.parseKeys("${PLUG_PREFIX}$keys")
      val keysCommand = injector.parser.parseKeys(command(keys))
      
      VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NVO, keysCommand, OWNER, AceVimHandler(mode), false)
      VimExtensionFacade.putKeyMapping(MappingMode.NVO, keysPrefixed, OWNER, keysCommand, true)
    }
    
    private fun command(name: String): String {
      return "<Plug>(acejump-$name)"
    }
  }
  
  override fun getName(): String {
    return NAME
  }
  
  override fun init() {
    register("<Space>", AceVimMode.JumpAllEditors)
    register("s", AceVimMode.JumpAllEditors)
    
    register("f", AceVimMode.Jump(StandardBoundaries.AFTER_CARET))
    register("F", AceVimMode.Jump(StandardBoundaries.BEFORE_CARET))
    register("t", AceVimMode.JumpTillForward(StandardBoundaries.AFTER_CARET))
    register("T", AceVimMode.JumpTillBackward(StandardBoundaries.BEFORE_CARET))
    
    register("w", AceVimMode.JumpToPattern(Pattern.VIM_LWORD, StandardBoundaries.AFTER_CARET))
    register("W", AceVimMode.JumpToPattern(Pattern.VIM_UWORD, StandardBoundaries.AFTER_CARET))
    register("b", AceVimMode.JumpToPattern(Pattern.VIM_LWORD, StandardBoundaries.BEFORE_CARET))
    register("B", AceVimMode.JumpToPattern(Pattern.VIM_UWORD, StandardBoundaries.BEFORE_CARET))
    register("e", AceVimMode.JumpToPattern(Pattern.VIM_LWORD_END, StandardBoundaries.AFTER_CARET))
    register("E", AceVimMode.JumpToPattern(Pattern.VIM_LWORD_END, StandardBoundaries.AFTER_CARET))
    register("ge", AceVimMode.JumpToPattern(Pattern.VIM_UWORD_END, StandardBoundaries.BEFORE_CARET))
    register("gE", AceVimMode.JumpToPattern(Pattern.VIM_UWORD_END, StandardBoundaries.BEFORE_CARET))
    
    register("j", AceVimMode.JumpToPattern(Pattern.LINE_INDENTS, StandardBoundaries.AFTER_CARET))
    register("k", AceVimMode.JumpToPattern(Pattern.LINE_INDENTS, StandardBoundaries.BEFORE_CARET))
    
    register("l", AceVimMode.Jump(StandardBoundaries.AFTER_CARET.intersection(StandardBoundaries.CARET_LINE)))
    register("h", AceVimMode.Jump(StandardBoundaries.BEFORE_CARET.intersection(StandardBoundaries.CARET_LINE)))
  }
}
