package com.littlewords.app

import android.app.Application
import com.littlewords.app.data.ReadingDatabase
import com.littlewords.app.data.ReadingRepository

class LittleWordsApplication : Application() {
    val repository: ReadingRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ReadingRepository(ReadingDatabase.open(this))
    }
}
