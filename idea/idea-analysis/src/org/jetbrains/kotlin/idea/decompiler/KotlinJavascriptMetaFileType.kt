/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.decompiler

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon
import kotlin.platform.platformStatic

public class KotlinJavascriptMetaFileType : FileType {
    companion object {
        platformStatic
        public val INSTANCE: KotlinJavascriptMetaFileType = KotlinJavascriptMetaFileType()
    }

    private val ICON = object : NotNullLazyValue<Icon>() {
        override fun compute(): Icon {
            return AllIcons.FileTypes.JavaClass
        }
    }

    override fun getName(): String {
        return "META"
    }

    override fun getDescription(): String {
        return IdeBundle.message("filetype.description.class")
    }

    override fun getDefaultExtension(): String {
        return "meta"
    }

    override fun getIcon(): Icon? {
        return ICON.getValue()
    }

    override fun isBinary(): Boolean {
        return true
    }

    override fun isReadOnly(): Boolean {
        return false
    }

    override fun getCharset(file: VirtualFile, content: ByteArray): String? {
        return null
    }

}