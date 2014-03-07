package net.shadowmage.ancientwarfare.core.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase.ActivationEvent;
import net.shadowmage.ancientwarfare.core.gui.Listener;
import net.shadowmage.ancientwarfare.core.util.RenderTools;

import org.lwjgl.opengl.GL11;

public class Button extends GuiElement
{

FontRenderer fr;
String text;
int textX;
int textY;

public Button(int topLeftX, int topLeftY, int width, int height, String text)
  {
  super(topLeftX, topLeftY, width, height);  
  this.text = text;
  this.addNewListener(new Listener(Listener.MOUSE_UP)
    {      
    @Override
    public boolean onEvent(GuiElement widget, ActivationEvent evt)
      {
      if(enabled && visible && isMouseOverElement(evt.mx, evt.my))
        {        
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
        onPressed();
        }
      return true;
      }
    });
  
  fr = Minecraft.getMinecraft().fontRenderer;
  int tw = fr.getStringWidth(text);
  textX = (width - tw)/2;
  textY = (height - 8)/2; 
  }

@Override
public void render(int mouseX, int mouseY, float partialTick)
  {
  if(visible)
    {
    Minecraft.getMinecraft().renderEngine.bindTexture(widgetTexture1);   
    int textureSize = 256;
    int startX = 0;
    int startY = enabled ? isMouseOverElement(mouseX, mouseY) ? 80 : 40 : 0;
    int textColor = startY==80 ? 0xa0a0a0ff : 0xffffffff;//grey or white
    int usedWidth = 256;
    int usedHeight = 40;  
    RenderTools.renderQuarteredTexture(textureSize, textureSize, startX, startY, usedWidth, usedHeight, renderX, renderY, width, height);
    fr.drawStringWithShadow(text, renderX+textX, renderY+textY, textColor);
    GL11.glColor4f(1.f, 1.f, 1.f, 1.f);
    }  
  }

/**
 * sub-classes may override this as an on-pressed callback
 * method is called whenever the 'pressed' sound is played
 */
protected void onPressed()
  {
  
  }

}
