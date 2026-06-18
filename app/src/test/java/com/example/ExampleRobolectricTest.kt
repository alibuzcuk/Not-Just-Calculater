package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NOT JUST Calculator", appName)
  }

  @Test
  fun `test large calculations and scientific e notation`() {
    val viewModel = CalculatorViewModel()
    
    // Test entering a very large number (20 digits long)
    val number1 = "69999999999999999999"
    number1.forEach { char ->
      viewModel.onKeyPressed(char.toString())
    }
    assertEquals("69999999999999999999", viewModel.expression.value)
    
    // Multiply by another very large number
    viewModel.onKeyPressed("×")
    val number2 = "99999999999999999999"
    number2.forEach { char ->
      viewModel.onKeyPressed(char.toString())
    }
    
    // Evaluate the expression
    viewModel.onKeyPressed("=")
    
    // Check that result format uses scientific notation, resulting in 7e39
    assertEquals("7e39", viewModel.expression.value)
  }
}
