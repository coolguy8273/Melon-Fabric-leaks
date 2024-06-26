package melon.system.antileak

import dev.zenhao.melon.Melon.Companion.verifiedState
import dev.zenhao.melon.utils.math.RandomUtil
import melon.system.antileak.checks.AntiLeakCheck
import melon.system.antileak.checks.PackageCheck
import melon.system.antileak.checks.ProcessCheck

object AntiLeak {
    private val needCheck = mutableListOf<AntiLeakCheck>()

    fun init() {
        needCheck.add(ProcessCheck)
        needCheck.add(PackageCheck)
        needCheck.forEach {
            if (it.isNotSafe()) {
                verifiedState = RandomUtil.nextInt(-1, Int.MIN_VALUE)
            }
        }
    }
}
