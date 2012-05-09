package org.bone.ircballoon

import org.eclipse.swt.widgets.{List => SWTList, _}
import org.eclipse.swt.layout._
import org.eclipse.swt.events._
import org.eclipse.swt.graphics._
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.custom.StackLayout

import org.eclipse.swt._

import scala.util.Random

class BalloonSetting(parent: Composite, onModify: ModifyEvent => Any) extends 
      Composite(parent, SWT.NONE) with SWTHelper
{
    var bgColor: Color = MyColor.Black
    var fgColor: Color = MyColor.White
    var messageFont: Font = Display.getDefault.getSystemFont

    val gridLayout = new GridLayout(4, false)
    val locationX = createText(this, "視窗位址 X：")
    val locationY = createText(this, "視窗位址 Y：")
    val width = createText(this, "視窗寬度：")
    val height = createText(this, "視窗高度：")
    val (bgLabel, bgButton) = createColorChooser(this, "背景顏色：", bgColor, bgColor = _)
    val (fgLabel, fgButton) = createColorChooser(this, "文字顏色：", fgColor, fgColor = _)
    val (fontLabel, fontButton) = createFontChooser(this, "訊息字型：", messageFont = _)
    val (transparentLabel, transparentScale) = createScaleChooser(this, "透明度：")
    val (displayTimeLabel, displayTimeSpinner) = createSpinner(this, "訊息停留秒數：", 1, 120)
    val (fadeTimeLabel, fadeTimeSpinner) = createSpinner(this, "淡入淡出效果時間(ms)：", 1, 5000)
    val (spacingLabel, spacingSpinner) = createSpinner(this, "泡泡間距（像素）：", 1, 20)
    val previewButton = createPreviewButton()

    def createSpanLabel() = {
        val label = new Label(this, SWT.NONE)
        val layoutData = new GridData(SWT.FILL, SWT.NONE, true, false)
        layoutData.horizontalSpan = 2
        label.setLayoutData(layoutData)
    }

    class TestThread(balloonController: BalloonController) extends Thread
    {
        private var shouldStop = false

        def setStop(shouldStop: Boolean)
        {
            this.shouldStop = shouldStop
        }

        def randomPause = (Random.nextInt(3) + 1) * 1000
        
        override def run ()
        {
            var count = 1

            while (!shouldStop) {
                val message = MessageSample.random(1).head
                balloonController.addMessage("[%d] %s" format(count, message))
                count = (count + 1)
                Thread.sleep(randomPause)
            }
        }
    }

    def createBalloonController() = 
    {
        val size = (width.getText.toInt, height.getText.toInt)
        val location = (locationX.getText.toInt, locationY.getText.toInt)
        val alpha = 255 - (255 * (transparentScale.getSelection / 100.0)).toInt

        BalloonController(
            size, location, 
            MyColor.White, bgColor, alpha, 
            fgColor, messageFont, 
            displayTimeSpinner.getSelection * 1000, 
            fadeTimeSpinner.getSelection,
            spacingSpinner.getSelection
        )
    }

    def setDefaultValue()
    {
        locationX.setText("100")
        locationY.setText("100")
        width.setText("300")
        height.setText("500")
        displayTimeSpinner.setSelection(5)
        fadeTimeSpinner.setSelection(500)
        spacingSpinner.setSelection(5)
    }

    def setTextVerify()
    {
        locationX.addVerifyListener { e: VerifyEvent => e.doit = e.text.forall(_.isDigit) }
        locationY.addVerifyListener { e: VerifyEvent => e.doit = e.text.forall(_.isDigit) }
        width.addVerifyListener { e: VerifyEvent => e.doit = e.text.forall(_.isDigit) }
        height.addVerifyListener { e: VerifyEvent => e.doit = e.text.forall(_.isDigit) }
    }

    def createPreviewButton() =
    {
        var balloonController: Option[BalloonController] = None
        var testThread: Option[TestThread] = None

        val layoutData = new GridData(SWT.FILL, SWT.NONE, true, false)
        val button = new Button(this, SWT.PUSH)

        def startPreview ()
        {
            balloonController = Some(createBalloonController)
            balloonController.foreach{ controller => 
                controller.open()
                testThread = Some(new TestThread(controller))
                testThread.foreach(_.start)
            }
            button.setText("停止預覽")
        }

        def stopPreview()
        {
            button.setText("開始預覽")
            balloonController.foreach{ controller =>
                testThread.foreach{_.setStop(true)}
                testThread = None
                controller.close()
            }
            balloonController = None
        }

        layoutData.horizontalSpan = 2
        button.setLayoutData(layoutData)
        button.setText("開始預覽")
        button.addSelectionListener { e: SelectionEvent =>
            balloonController match {
                case None    => startPreview()
                case Some(x) => stopPreview()
            }
        }
        button
    }

    def isSettingOK = {
        locationX.getText.trim.length > 0 &&
        locationY.getText.trim.length > 0 &&
        width.getText.trim.length > 0 &&
        height.getText.trim.length > 0
    }

    def setModifyListener() {
        locationX.addModifyListener(onModify)
        locationY.addModifyListener(onModify)
        width.addModifyListener(onModify)
        height.addModifyListener(onModify)
    }

    def setUIEnabled(isEnabled: Boolean)
    {
        locationX.setEnabled(isEnabled)
        locationY.setEnabled(isEnabled)
        width.setEnabled(isEnabled)
        height.setEnabled(isEnabled)
        bgButton.setEnabled(isEnabled)
        fgButton.setEnabled(isEnabled)
        fontButton.setEnabled(isEnabled)
        transparentScale.setEnabled(isEnabled)
        displayTimeSpinner.setEnabled(isEnabled)
        fadeTimeSpinner.setEnabled(isEnabled)
        spacingSpinner.setEnabled(isEnabled)
        previewButton.setEnabled(isEnabled)
    }

    this.setLayout(gridLayout)
    this.setDefaultValue()
    this.setTextVerify()
    this.setModifyListener()
}

