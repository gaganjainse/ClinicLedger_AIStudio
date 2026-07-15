package com.villageclinicledger.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.ui.util.LocaleManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyTreeDialog(
    familyName: String,
    members: List<Patient>,
    onDismiss: () -> Unit,
    onNavigateToPatient: (Long) -> Unit
) {
    val isHindi = LocaleManager.LocalIsHindi.current
    
    // Categorize members into generations based on relationships
    val genMap = remember(members) { categorizeFamilyMembers(members) }
    val gen1 = genMap[1] ?: emptyList()
    val gen2 = genMap[2] ?: emptyList()
    val gen3 = genMap[3] ?: emptyList()
    val gen4 = genMap[4] ?: emptyList()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header TopAppBar
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isHindi) "$familyName - वंशावली" else "$familyName Family Tree",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isHindi) "कुल सदस्य: ${members.size}" else "Total Members: ${members.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Main Tree Layout (Scrollable both horizontally & vertically)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    
                    // Welcome & Info banner
                    Text(
                        text = if (isHindi) "पारस्परिक संबंधों को देखने के लिए किसी भी कार्ड पर टैप करें।" else "Tap any member card to view their medical profile ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (members.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isHindi) "कोई सदस्य नहीं मिला" else "No family members listed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Generation 1: Grandparents
                        if (gen1.isNotEmpty()) {
                            GenerationLayer(
                                title = if (isHindi) "दादा-दादी / नाना-नानी (Grandparents)" else "Generation I: Grandparents",
                                badgeColor = MaterialTheme.colorScheme.primary,
                                badgeTextColor = MaterialTheme.colorScheme.onPrimary,
                                members = gen1,
                                onMemberClick = {
                                    onDismiss()
                                    onNavigateToPatient(it.id)
                                }
                            )
                            ConnectorLine()
                        }

                        // Generation 2: Parents
                        if (gen2.isNotEmpty()) {
                            GenerationLayer(
                                title = if (isHindi) "माता-पिता / अभिभावक (Parents)" else "Generation II: Parents & Uncles/Aunts",
                                badgeColor = MaterialTheme.colorScheme.secondary,
                                badgeTextColor = MaterialTheme.colorScheme.onSecondary,
                                members = gen2,
                                onMemberClick = {
                                    onDismiss()
                                    onNavigateToPatient(it.id)
                                }
                            )
                            ConnectorLine()
                        }

                        // Generation 3: Self / Siblings
                        if (gen3.isNotEmpty()) {
                            GenerationLayer(
                                title = if (isHindi) "स्वयं और भाई-बहन (Self / Siblings)" else "Generation III: Self & Siblings",
                                badgeColor = MaterialTheme.colorScheme.tertiary,
                                badgeTextColor = MaterialTheme.colorScheme.onTertiary,
                                members = gen3,
                                onMemberClick = {
                                    onDismiss()
                                    onNavigateToPatient(it.id)
                                }
                            )
                            if (gen4.isNotEmpty()) {
                                ConnectorLine()
                            }
                        }

                        // Generation 4: Children
                        if (gen4.isNotEmpty()) {
                            GenerationLayer(
                                title = if (isHindi) "बच्चे (Children)" else "Generation IV: Children & Nephews/Nieces",
                                badgeColor = MaterialTheme.colorScheme.errorContainer,
                                badgeTextColor = MaterialTheme.colorScheme.onErrorContainer,
                                members = gen4,
                                onMemberClick = {
                                    onDismiss()
                                    onNavigateToPatient(it.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GenerationLayer(
    title: String,
    badgeColor: Color,
    badgeTextColor: Color,
    members: List<Patient>,
    onMemberClick: (Patient) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Generation Label Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = badgeColor.copy(alpha = 0.15f),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // Horizontal Row of family members in this generation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            members.forEachIndexed { index, member ->
                FamilyMemberCard(member = member, onClick = { onMemberClick(member) })
                if (index < members.size - 1) {
                    // Symmetrical branch divider between horizontal cards of same level
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@Composable
fun FamilyMemberCard(
    member: Patient,
    onClick: () -> Unit
) {
    val localizedName = LocaleManager.formatPatientName(member.name)
    val initials = localizedName.split(" ").filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.take(1).uppercase() }

    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Profile Initial Avatar Circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Member Name (Automatic Aa Bb type title case format is applied)
            Text(
                text = localizedName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Relationship label pill
            val relDisplay = member.relationship.ifBlank { "Member" }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = LocaleManager.getLocalizedText(relDisplay),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Individual Running Balance
            Text(
                text = LocaleManager.formatCurrency(member.currentBalance),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = if (member.currentBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ConnectorLine() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight(0.65f)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}

fun categorizeFamilyMembers(members: List<Patient>): Map<Int, List<Patient>> {
    val gen1 = mutableListOf<Patient>()
    val gen2 = mutableListOf<Patient>()
    val gen3 = mutableListOf<Patient>()
    val gen4 = mutableListOf<Patient>()

    for (m in members) {
        val rel = m.relationship.lowercase().trim()
        
        val isGen1 = rel.contains("grand") || 
                     rel.contains("daada") || 
                     rel.contains("daadi") || 
                     rel.contains("naana") || 
                     rel.contains("naani") || 
                     rel.contains("दादा") || 
                     rel.contains("दादी") || 
                     rel.contains("नाना") || 
                     rel.contains("नानी") || 
                     rel.contains("बाबा")

        val isGen2 = rel.contains("father") || 
                     rel.contains("mother") || 
                     rel.contains("uncle") || 
                     rel.contains("aunt") || 
                     rel.contains("husband") || 
                     rel.contains("wife") || 
                     rel.contains("pita") || 
                     rel.contains("mata") || 
                     rel.contains("chacha") || 
                     rel.contains("chachi") || 
                     rel.contains("mummy") || 
                     rel.contains("papa") || 
                     rel.contains("पिता") || 
                     rel.contains("माता") || 
                     rel.contains("चाचा") || 
                     rel.contains("चाची") || 
                     rel.contains("पति") || 
                     rel.contains("पत्नी")

        val isGen4 = rel.contains("son") || 
                     rel.contains("daughter") || 
                     rel.contains("nephew") || 
                     rel.contains("niece") || 
                     rel.contains("child") || 
                     rel.contains("beta") || 
                     rel.contains("beti") || 
                     rel.contains("बेटा") || 
                     rel.contains("बेटी") || 
                     rel.contains("भतीजा") || 
                     rel.contains("भतीजी") || 
                     rel.contains("पुत्र") || 
                     rel.contains("पुत्री")

        if (isGen1) {
            gen1.add(m)
        } else if (isGen2) {
            gen2.add(m)
        } else if (isGen4) {
            gen4.add(m)
        } else {
            gen3.add(m)
        }
    }
    return mapOf(
        1 to gen1,
        2 to gen2,
        3 to gen3,
        4 to gen4
    )
}
