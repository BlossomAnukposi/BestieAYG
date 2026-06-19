package com.bayg.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bayg

@Composable
fun NavBar(navController: NavController) {
    Row (
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.bayg.card)
            .height(100.dp)
            .fillMaxWidth()
            .padding(50.dp, 10.dp)
    ) {
        NavButton("Home", "dashboard", navController)
        NavButton("Stats", "stats", navController)
        NavButton("Settings", "settings", navController)
    }
}

@Composable
fun NavButton(name: String, route: String, navController: NavController) {
    var active: Boolean = navController.currentDestination?.route == route
    val types: Map<String, Pair<Int, Int>> = mapOf(
        "Home" to Pair
            (com.bayg.R.drawable.home, com.bayg.R.drawable.homegreen),
        "Stats" to Pair
            (com.bayg.R.drawable.stats, com.bayg.R.drawable.statsgreen),
        "Settings" to Pair
            (com.bayg.R.drawable.settings, com.bayg.R.drawable.settingsgreen))

    require(types.keys.contains(name)) {
        "Invalid type provided"
    }

    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally){
        if(active) {
            Image(
                painter = painterResource(id = types.getValue(name).second),
                contentDescription = name,
                modifier = Modifier.height(40.dp).width(45.dp)
            )

            Paragraph(name, MaterialTheme.bayg.green, true)
        }
        else {
            Image(
                painter = painterResource(id = types.getValue(name).first),
                contentDescription = name,
                modifier = Modifier
                    .clickable(onClick = { navController.navigate(route) })
                    .height(45.dp).width(45.dp)
            )

            Paragraph(name, MaterialTheme.bayg.white)
        }
    }
}