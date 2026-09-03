package com.archimede.w2full.data.mimit

interface MimitDataSource : MimitStationsDataSource {
    fun downloadPrices(): MimitDataset<MimitPrice>
}
