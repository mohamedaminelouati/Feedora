@file:Suppress("SpellCheckingInspection")

package com.mohamedaminelouati.feedora.ui.ext

import com.mohamedaminelouati.feedora.BuildConfig

const val GITHUB = "github"
const val FDROID = "fdroid"
const val GOOGLE_PLAY = "googlePlay"

const val isFDroid = BuildConfig.FLAVOR == FDROID
const val isGitHub = BuildConfig.FLAVOR == GITHUB
const val isGooglePlay = BuildConfig.FLAVOR == GOOGLE_PLAY
