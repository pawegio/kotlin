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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.testFramework.BinaryLightVirtualFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import kotlin.platform.platformStatic

// TODO refactoring
public object JsMetaFileUtils {
    platformStatic
    public fun getRelativeToRootPath(file: VirtualFile): String {
        val root = getRoot(file)
        return VfsUtilCore.getRelativePath(file, root)!!
    }

    platformStatic
    public fun getClassFqName(relPath: String): FqName {
        val index = relPath.indexOf(VfsUtilCore.VFS_SEPARATOR_CHAR)
        val pathToFile = relPath.substring(index+1)

        val index1 = pathToFile.lastIndexOf('.')
        val name = if (index1 >= 0) pathToFile.substring(0, index1) else pathToFile
        return FqName(name.replace('/', '.'))
    }

    platformStatic
    public fun getClassId(relPath: String): ClassId {
        val classFqName = getClassFqName(relPath)
        val shortName = classFqName.shortName().asString()
        val index1 = shortName.lastIndexOf('.')
        val name = if (index1 >= 0) shortName.substring(0, index1) else shortName
        val packageFqName = getPackageFqName(relPath)
        return ClassId(packageFqName, FqName(name), false)
    }

    platformStatic
    public fun getPackageFqName(relPath: String): FqName {
        val index = relPath.indexOf(VfsUtilCore.VFS_SEPARATOR_CHAR)
        val pathToFile = relPath.substring(index+1)
        if (pathToFile == "_DefaultPackage.meta") return FqName.ROOT

        val index1 = pathToFile.lastIndexOf('/')
        val name = if (index1 >= 0) pathToFile.substring(0, index1) else pathToFile
        return FqName(name.replace('/', '.'))
    }

    platformStatic
    public fun isPackageHeader(relPath: String): Boolean {
        val classFqName = JsMetaFileUtils.getClassFqName(relPath)
        return PackageClassUtils.isPackageClassFqName(classFqName)
    }

    platformStatic
    public fun getModuleName(relPath: String): String {
        val index = relPath.indexOf(VfsUtilCore.VFS_SEPARATOR_CHAR)
        return relPath.substring(0, index)
    }

    private fun getRoot(file: VirtualFile): VirtualFile {
        if (file.getParent() == null) return file else return getRoot(file.getParent())
    }
}

