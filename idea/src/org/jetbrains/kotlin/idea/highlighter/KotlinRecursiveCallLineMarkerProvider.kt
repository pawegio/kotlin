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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.isInlined
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import java.util.HashSet

public class KotlinRecursiveCallLineMarkerProvider() : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement) = null

    override fun collectSlowLineMarkers(elements: MutableList<PsiElement>, result: MutableCollection<LineMarkerInfo<*>>) {
        val markedLineNumbers = HashSet<Int>()
        for (element in elements) {
            ProgressManager.checkCanceled()
            if (element is JetElement) {
                val lineNumber = element.getLineNumber()
                if (lineNumber !in markedLineNumbers && isRecursiveCall(element)) {
                    markedLineNumbers.add(lineNumber)
                    result.add(RecursiveMethodCallMarkerInfo(element))
                }
            }
        }
    }

    private fun getEnclosingFunction(element: JetElement): JetNamedFunction? {
        for (parent in element.parents(false)) {
            when (parent) {
                is JetFunctionLiteral -> if (!parent.isInlined(parent.analyze())) return null
                is JetNamedFunction -> return parent
                is JetClassOrObject -> return null
            }
        }
        return null
    }

    private fun isRecursiveCall(element: JetElement): Boolean {
        if (!(element is JetSimpleNameExpression && element.getParent() is JetCallExpression || element is JetArrayAccessExpression))
            return false
        val enclosingFunction = getEnclosingFunction(element) ?: return false

        val bindingContext = element.analyze()
        val enclosingFunctionDescriptor = bindingContext[BindingContext.FUNCTION, enclosingFunction] ?: return false

        val call = bindingContext[BindingContext.CALL, element] ?: return false
        val resolvedCall = bindingContext[BindingContext.RESOLVED_CALL, call] ?: return false

        if (resolvedCall.getCandidateDescriptor().getOriginal() != enclosingFunctionDescriptor) return false

        val extensionReceiver = resolvedCall.getExtensionReceiver()
        if (extensionReceiver is ExpressionReceiver) {
            val thisTarget = (extensionReceiver.getExpression() as? JetThisExpression)?.getInstanceReference()?.getReference()?.resolve()
            if (thisTarget != enclosingFunction) {
                return false
            }
        }

        val dispatchReceiver = resolvedCall.getDispatchReceiver()
        if (dispatchReceiver is ExpressionReceiver) {
            val thisTarget = (dispatchReceiver.getExpression() as? JetThisExpression)?.getInstanceReference()?.getReference()?.resolve()
            if (thisTarget != enclosingFunction.getNonStrictParentOfType<JetClassOrObject>()) {
                return false
            }
        }

        return true
    }

    private class RecursiveMethodCallMarkerInfo(callElement: JetElement)
            : LineMarkerInfo<JetElement>(
                    callElement,
                    callElement.getTextRange(),
                    AllIcons.Gutter.RecursiveMethod,
                    Pass.UPDATE_OVERRIDEN_MARKERS,
                    { "Recursive call" },
                    null,
                    GutterIconRenderer.Alignment.RIGHT
    ) {

        override fun createGutterRenderer(): GutterIconRenderer? {
            return object : LineMarkerInfo.LineMarkerGutterIconRenderer<JetElement>(this) {
                override fun getClickAction() = null // to place breakpoint on mouse click
            }
        }
    }

}

private fun PsiElement.getLineNumber(): Int {
    return PsiDocumentManager.getInstance(getProject()).getDocument(getContainingFile())!!.getLineNumber(getTextOffset())
}
