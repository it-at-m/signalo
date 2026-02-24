package com.example.test.signalo

import timber.log.Timber

class CustomDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        val className = element.className.substringAfterLast('.')
        val methodName = element.methodName
        //return "$className($methodName)"
        return className
    }
}