package com.localentertainmentgames.homeinventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.localentertainmentgames.homeinventory.ui.icons.AppIcons
import com.localentertainmentgames.homeinventory.ui.theme.AppTheme
import com.localentertainmentgames.homeinventory.ui.theme.PillShape
import com.localentertainmentgames.homeinventory.ui.theme.Radius

@Composable
fun leFieldColors(focusedBorder: Color = AppTheme.colors.cta) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = focusedBorder,
    unfocusedBorderColor = AppTheme.colors.border,
    focusedContainerColor = AppTheme.colors.bg,
    unfocusedContainerColor = AppTheme.colors.surface,
    focusedTextColor = AppTheme.colors.fg1,
    unfocusedTextColor = AppTheme.colors.fg1,
    cursorColor = AppTheme.colors.cta,
    focusedPlaceholderColor = AppTheme.colors.fg4,
    unfocusedPlaceholderColor = AppTheme.colors.fg4,
    errorBorderColor = AppTheme.colors.error,
)

/** OutlinedTextField with a leading search icon. */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().testTag("search"),
        placeholder = { Text(placeholder, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = AppTheme.colors.fg4, modifier = Modifier.size(22.dp)) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                androidx.compose.material3.IconButton(onClick = { onValueChange("") }) {
                    Icon(AppIcons.Close, contentDescription = "Clear search", tint = AppTheme.colors.fg3, modifier = Modifier.size(20.dp))
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(Radius.md),
        colors = leFieldColors(),
        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
    )
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = AppTheme.text.fieldLabel, color = AppTheme.colors.fg1, modifier = modifier)
}

/** Labelled text input used in the add/edit form. */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helper: String? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    textStyle: TextStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
    singleLine: Boolean = true,
    tag: String = label,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().testTag("field-$tag"),
            placeholder = { Text(placeholder, style = textStyle) },
            singleLine = singleLine,
            isError = error != null,
            shape = RoundedCornerShape(Radius.md),
            colors = leFieldColors(),
            textStyle = textStyle,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
        val msg = error ?: helper
        if (msg != null) {
            Text(
                msg,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = if (error != null) AppTheme.colors.error else AppTheme.colors.fg4,
            )
        }
    }
}

/** A tappable read-only field (location, category, dates) with a trailing icon. */
@Composable
fun SelectField(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: ImageVector = AppIcons.ChevronRight,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(Radius.md),
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("select-$label"),
        ) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    Icon(leading, contentDescription = null, tint = AppTheme.colors.fg3, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    value ?: placeholder,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = if (value == null) AppTheme.colors.fg4 else AppTheme.colors.fg1,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Icon(trailing, contentDescription = null, tint = AppTheme.colors.fg4, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** FilterChip for tags: filled brand purple with a check icon when selected. */
@Composable
fun TagFilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = AppTheme.text.buttonSmall) },
        leadingIcon = if (selected) {
            { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        modifier = modifier.height(40.dp).testTag("chip-$label"),
        shape = PillShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = AppTheme.colors.fg2,
            selectedContainerColor = AppTheme.colors.brand,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppTheme.colors.borderStrong,
            selectedBorderColor = AppTheme.colors.brand,
            borderWidth = 1.5.dp,
            selectedBorderWidth = 1.5.dp,
        ),
    )
}

/** Room / Category filter: FilterChip that opens a DropdownMenu. */
@Composable
fun DropdownChip(
    label: String,
    selected: String?,
    options: List<String>,
    allLabel: String,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        FilterChip(
            selected = selected != null,
            onClick = { open = true },
            label = { Text(selected ?: label, style = AppTheme.text.buttonSmall) },
            trailingIcon = { Icon(AppIcons.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.height(40.dp).testTag("dropdown-$label"),
            shape = PillShape,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = AppTheme.colors.fg2,
                iconColor = AppTheme.colors.fg2,
                selectedContainerColor = AppTheme.colors.brand,
                selectedLabelColor = Color.White,
                selectedTrailingIconColor = Color.White,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected != null,
                borderColor = AppTheme.colors.borderStrong,
                selectedBorderColor = AppTheme.colors.brand,
                borderWidth = 1.5.dp,
                selectedBorderWidth = 1.5.dp,
            ),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(null); open = false })
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); open = false },
                    trailingIcon = if (opt == selected) {
                        { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}
