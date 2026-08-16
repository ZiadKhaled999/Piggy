import sys

with open('app/src/main/java/com/oryno/piggy_ledger/ui/BackupSettingsView.kt', 'r') as f:
    content = f.read()

# Imports
imports_to_add = """import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
"""

if "import androidx.compose.animation.animateColorAsState" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\n" + imports_to_add)

# 1. Update currentColor to be animated
content = content.replace(
    "val currentColor = colorMap[exportType] ?: Color(0xFF10A37F)",
    "val currentColor by animateColorAsState(targetValue = colorMap[exportType] ?: Color(0xFF10A37F), animationSpec = tween(400))"
)

# 2. Add scale animation to Logo Box based on type
logo_box_replacement = """
        val scale by animateFloatAsState(
            targetValue = if (exportType == "CSV") 1.05f else if (exportType == "JSON") 0.95f else 1f,
            animationSpec = tween(300)
        )
        // Logo
        Box(
            modifier = Modifier.size(100.dp, 120.dp).androidx.compose.ui.draw.scale(scale),
            contentAlignment = Alignment.Center
        )"""
content = content.replace(
    """        // Logo
        Box(
            modifier = Modifier.size(100.dp, 120.dp),
            contentAlignment = Alignment.Center
        )""",
    logo_box_replacement.replace("androidx.compose.ui.draw.scale", "androidx.compose.ui.draw.scale")
)
# Wait, let's just add the import for scale
if "import androidx.compose.ui.draw.scale" not in content:
    content = content.replace("import androidx.compose.ui.draw.scale\n", "")
    content = content.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.draw.scale\nimport androidx.compose.ui.draw.clip")

# 3. Shorten subtitle
content = content.replace(
    '"You can export your transactions and use them in Excel, Numbers, Google Spreadsheets etc."',
    '"Export your data for external spreadsheets."'
)

# 4. FillMaxHeight on root column and add weight for spacer
content = content.replace(
    "modifier = Modifier\n            .fillMaxWidth()\n            .padding(top = 24.dp)",
    "modifier = Modifier\n            .fillMaxSize()\n            .padding(top = 24.dp)"
)
content = content.replace(
    "Spacer(modifier = Modifier.height(24.dp))\n        \n        Text(\n            text = \"FOUND",
    "Spacer(modifier = Modifier.weight(1f))\n        \n        Text(\n            text = \"FOUND"
)

# 5. Replace Navigation Tabs logic
old_tabs = """        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("JSON", "CSV", "EXCEL").forEach { type ->
                val isSelected = exportType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Color.White else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { exportType = type }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) currentColor else TextLight,
                        fontSize = 14.sp
                    )
                }
            }
        }"""

new_tabs = """        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("JSON", "CSV", "EXCEL").forEach { type ->
                val isSelected = exportType == type
                
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) PinkPrimary.copy(alpha = 0.08f) else Color.Transparent,
                    animationSpec = tween(300)
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) PinkPrimary else Color(0xFFE0E0E0),
                    animationSpec = tween(300)
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) PinkPrimary else TextLight,
                    animationSpec = tween(300)
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(bgColor, RoundedCornerShape(12.dp))
                        .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { exportType = type }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        fontSize = 15.sp
                    )
                }
            }
        }"""

content = content.replace(old_tabs, new_tabs)

# 6. Change Switch color
content = content.replace("checkedTrackColor = NavyDark", "checkedTrackColor = PinkPrimary")

# 7. Change Export Button Color
content = content.replace("containerColor = Color.Black", "containerColor = PinkPrimary")

with open('app/src/main/java/com/oryno/piggy_ledger/ui/BackupSettingsView.kt', 'w') as f:
    f.write(content)

