package com.archimede.w2full.data.mimit

fun interface MimitStationsDataSource {
    fun downloadStations(): MimitDataset<MimitStation>
}
