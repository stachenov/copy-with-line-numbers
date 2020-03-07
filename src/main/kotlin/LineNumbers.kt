package name.tachenov.intellij.plugins.copyWithLineNumbers

internal fun lineNumbers(count: Int) =
        (1..count).map { n -> String.format("%0${count.toString().length}d", n) }
