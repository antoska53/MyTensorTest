package com.example.mytensortest.ui

sealed class ProgressStatus
object Loading : ProgressStatus()
object Success : ProgressStatus()
