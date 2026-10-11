package com.mustafa.gardener.modules.vision

/**
 * COCO 80-class labels for pretrained YOLOv8n detection (Ultralytics order).
 */
object CocoLabels {
    val ALL: List<String> = listOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck",
        "boat", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench",
        "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra",
        "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove",
        "skateboard", "surfboard", "tennis racket", "bottle", "wine glass", "cup",
        "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
        "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse",
        "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
        "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
        "toothbrush",
    )

    /** Short Turkish aliases for frequent smoke-test classes. */
    private val TR_HINT: Map<String, String> = mapOf(
        "person" to "kişi",
        "car" to "araba",
        "bicycle" to "bisiklet",
        "motorcycle" to "motosiklet",
        "bus" to "otobüs",
        "truck" to "kamyon",
        "dog" to "köpek",
        "cat" to "kedi",
        "chair" to "sandalye",
        "bottle" to "şişe",
        "cup" to "bardak",
        "laptop" to "dizüstü",
        "cell phone" to "telefon",
        "tv" to "tv",
        "potted plant" to "saksı bitkisi",
    )

    fun displayLabel(english: String): String {
        val tr = TR_HINT[english] ?: return english
        return "$english ($tr)"
    }

    fun colorArgb(classIndex: Int): Int {
        // Distinct hues without purple-heavy palette; alpha ~0xE0 for stroke fill text bg.
        val palette = intArrayOf(
            0xE0E85D04.toInt(),
            0xE01B6CA8.toInt(),
            0xE02A9D8F.toInt(),
            0xE0C44536.toInt(),
            0xE0B08900.toInt(),
            0xE03D5A5B.toInt(),
            0xE0D97706.toInt(),
            0xE0156F6B.toInt(),
        )
        return palette[classIndex.mod(palette.size)]
    }
}
