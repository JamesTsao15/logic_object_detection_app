package com.example.logic_object_detection

import com.google.ar.core.Anchor
import com.google.ar.core.Pose

data class ARObject(val label:String,val pose: Pose)
