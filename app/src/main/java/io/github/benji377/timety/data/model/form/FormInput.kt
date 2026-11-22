package io.github.benji377.timety.data.model.form

/**
 * An optional input of an item editor that the user can switch on or off in Settings > Forms.
 * Each editor stores the inputs that are on by [storageValue], so an input added in a later
 * version starts off for everyone and is opt-in.
 */
sealed interface FormInput {
    val storageValue: String

    /** Whether a new install starts with this input on. */
    val newInstallOn: Boolean

    /**
     * Whether the editor had this input before Settings > Forms existed. An update from such a
     * version has nothing stored yet and keeps exactly these inputs on, so its forms don't change.
     * False for every input added since.
     */
    val existedBeforeForms: Boolean
}

/** Optional inputs of the task editors; one-off and recurring tasks share them. */
enum class TaskInput(
    override val storageValue: String,
    override val newInstallOn: Boolean,
    override val existedBeforeForms: Boolean,
) : FormInput {
    TAGS("tags", newInstallOn = true, existedBeforeForms = true),
    DESCRIPTION("description", newInstallOn = true, existedBeforeForms = true),
    PRIORITY_EFFORT("priorityEffort", newInstallOn = false, existedBeforeForms = true),
    DUE("due", newInstallOn = true, existedBeforeForms = true),

    /** Counts from the due date, so it is only on while [DUE] is on too. */
    REMINDERS("reminders", newInstallOn = true, existedBeforeForms = true),
    LOCATION("location", newInstallOn = false, existedBeforeForms = true),
    CHECKLIST("checklist", newInstallOn = true, existedBeforeForms = true),

    /** Recurring tasks only. */
    WINDOW("window", newInstallOn = false, existedBeforeForms = true),
}

/** Optional inputs of the habit editor. */
enum class HabitInput(
    override val storageValue: String,
    override val newInstallOn: Boolean,
    override val existedBeforeForms: Boolean,
) : FormInput {
    TAGS("tags", newInstallOn = true, existedBeforeForms = true),
    NOTES("notes", newInstallOn = true, existedBeforeForms = true),
    STACK("stack", newInstallOn = false, existedBeforeForms = true),
    ICON("icon", newInstallOn = true, existedBeforeForms = true),
    COLOR("color", newInstallOn = true, existedBeforeForms = true),
    TIMES_PER_DAY("timesPerDay", newInstallOn = false, existedBeforeForms = true),
    REMINDERS("reminders", newInstallOn = true, existedBeforeForms = true),
}

/** The switched-on inputs of one editor, as stored in settings. */
data class FormSkeleton<T>(val on: Set<T>) where T : Enum<T>, T : FormInput {

    fun isOn(input: T): Boolean = input in on

    /**
     * Whether the editor shows [input]: when it is on, or when the item already has a value for
     * it, so switching an input off never hides existing data.
     */
    fun shows(input: T, hasValue: Boolean): Boolean = isOn(input) || hasValue

    companion object {
        const val SEPARATOR = ","

        /**
         * Parses a stored value. Nothing stored means an update from before Settings > Forms,
         * which keeps the inputs it already had; unknown entries from newer versions are ignored.
         */
        inline fun <reified T> parse(stored: String?): FormSkeleton<T> where T : Enum<T>, T : FormInput {
            if (stored == null) return FormSkeleton(enumValues<T>().filterTo(mutableSetOf()) { it.existedBeforeForms })
            val values = stored.split(SEPARATOR).toSet()
            return FormSkeleton(enumValues<T>().filterTo(mutableSetOf()) { it.storageValue in values })
        }

        fun <T> serialize(on: Set<T>): String where T : Enum<T>, T : FormInput =
            on.sortedBy { it.ordinal }.joinToString(SEPARATOR) { it.storageValue }

        /** The inputs a new install starts with. */
        inline fun <reified T> newInstallPreset(): Set<T> where T : Enum<T>, T : FormInput =
            enumValues<T>().filterTo(mutableSetOf()) { it.newInstallOn }
    }
}
