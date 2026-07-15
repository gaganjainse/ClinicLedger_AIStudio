package com.villageclinicledger.ui.compose.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.ui.util.DateTimeUtils
import com.villageclinicledger.ui.util.LocaleManager
import java.util.Calendar

@Composable
fun MorningBriefSection(
    allPatients: List<Patient>,
    familyGroups: List<FamilyGroup>,
    totalCollectedToday: Double
) {
    var isExpanded by remember { mutableStateOf(false) }

    val totalOutstanding = allPatients.sumOf { it.currentBalance }
    val unlinkedPatientsCount = allPatients.count { it.familyGroupId == null }
    val activeFamiliesCount = familyGroups.size
    val highestDuePatient = allPatients.maxByOrNull { it.currentBalance }
    
    val dayHindi = DateTimeUtils.getLocalizedDayOfWeek(Calendar.getInstance().time, isHindi = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LightMode,
                            contentDescription = "Morning Brief",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "प्रणाम डॉक्टर साहब! / Welcome Doctor!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "आज $dayHindi की सुर्खियां / Today's Snapshots",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = "Toggle Brief",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    BriefInsightRow(
                        icon = Icons.Rounded.MonetizationOn,
                        label = "आज की कुल रिकवरी / Recovery Today",
                        value = "${LocaleManager.formatCurrency(totalCollectedToday)} जमा प्राप्त"
                    )

                    BriefInsightRow(
                        icon = Icons.Rounded.Groups,
                        label = "कुल बकाया नेटवर्क / Outstanding Ledger",
                        value = "${LocaleManager.formatCurrency(totalOutstanding)} बकाया ($activeFamiliesCount सक्रिय परिवार)"
                    )

                    if (highestDuePatient != null && highestDuePatient.currentBalance > 0) {
                        BriefInsightRow(
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            label = "अधिकतम बकाया मरीज / Highest Pending Patient",
                            value = "${highestDuePatient.name} (${LocaleManager.formatCurrency(highestDuePatient.currentBalance)})",
                            isError = true
                        )
                    }

                    if (unlinkedPatientsCount > 0) {
                        BriefInsightRow(
                            icon = Icons.Rounded.FamilyRestroom,
                            label = "बिना परिवार के मरीज / Unlinked Clinic Memories",
                            value = "$unlinkedPatientsCount मरीजों को परिवार से जोड़ना बाकी है।"
                        )
                    }
                }
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "टैप करें और आज की क्लिनिक मेमोरी का विवरण देखें।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BriefInsightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
