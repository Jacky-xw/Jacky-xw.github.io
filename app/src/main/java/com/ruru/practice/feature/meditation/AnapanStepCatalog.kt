package com.ruru.practice.feature.meditation

data class AnapanStep(val number:Int,val group:String,val title:String,val guidance:String)

object AnapanStepCatalog {
    val steps = listOf(
        AnapanStep(1,"身念处","入息、出息","吸气知道吸气，呼气知道呼气。保持自然，不控制呼吸。"),
        AnapanStep(2,"身念处","长、短","呼吸长时如实知道长，呼吸短时如实知道短。"),
        AnapanStep(3,"身念处","觉知息身全过程","觉知每一口呼吸从开始到结束的完整过程。"),
        AnapanStep(4,"身念处","身行止息","随着安住与放松，学习让呼吸相关的粗重身行逐渐止息。"),
        AnapanStep(5,"受念处","觉知喜","如有喜生起，知道喜；不追逐喜，也不要求喜出现。"),
        AnapanStep(6,"受念处","觉知乐","如有乐生起，知道乐；保持清楚，不染著于乐。"),
        AnapanStep(7,"受念处","觉知心行","如实知道当下感受与心行正在怎样变化。"),
        AnapanStep(8,"受念处","心行止息","在安定中学习令粗重的心行逐渐止息。"),
        AnapanStep(9,"心念处","觉知心","知道心此刻是散乱、安定、收缩、开放或处于其他状态。"),
        AnapanStep(10,"心念处","使心欣悦","以适当的方法令心明朗、欣悦，不强造特殊体验。"),
        AnapanStep(11,"心念处","使心安定","使心统一、安住、不散乱。"),
        AnapanStep(12,"心念处","使心得解脱","观察心从粗重执著中逐渐得到的自在与解脱。"),
        AnapanStep(13,"法念处","观察无常","直接观察呼吸、身体、感受、心念的出现、变化与消失。"),
        AnapanStep(14,"法念处","观察断","观察不善与执著如何减弱、断除；不以强迫压制替代如实观察。"),
        AnapanStep(15,"法念处","观察无欲","观察对经验的贪求如何减弱，心如何趋向离欲。"),
        AnapanStep(16,"法念处","观察灭","如实观察现象止息、寂灭的过程，不追求某种特殊境界。")
    )
    fun title(number:Int)=steps.firstOrNull{it.number==number}?.title ?: steps.first().title
}
