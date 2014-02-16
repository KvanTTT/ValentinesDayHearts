package com.app;

public class ColorUtils {
	public static float[] RgbToHsl(float r, float g, float b, float a) {
		float h = 0, s = 0, l = 0;

		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));

		// hue
		if(max == min)
		{
			h = 0; // undefined
		}
		else if (max == r && g >= b)
		{
			h = 1.0f / 6.0f * (g - b) / (max - min);
		}
		else if (max == r && g < b)
		{ 
			h = 1.0f / 6.0f * (g - b) / (max - min) + 1.0f;
		}
		else if (max == g)
		{
			h = 1.0f / 6.0f * (b - r) / (max - min) + 1.0f / 3.0f;
		}
		else if (max == b)
		{
			h = 1.0f / 6.0f * (r - g) / (max - min) + 2.0f / 3.0f;
		}

		// luminance
		l = (max + min) / 2.0f;

		// saturation
		if(l == 0 || max == min)
		{
			s = 0;
		}
		else if (0 < l && l <= 0.5)
		{
			s = (max - min) / (max + min);
		}
		else if (l > 0.5)
		{
			s = (max-min) / (2 - (max+min)); //(max-min > 0)?
		}

		float[] result = new float[4];
		result[0] = h;
		result[1] = s;
		result[2] = l;
		result[3] = a;
		
		return result;
	}
	
	public static float[] HslToRgb(float h, float s, float l, float a) {
		float[] result = new float[4];
		if (s == 0) {
			result[0] = l;
			result[1] = l;
			result[2] = l;
			result[3] = a;
		} else {
			float q = (l < 0.5f) ? (l * (1.0f + s)) : (l + s - (l * s));
			float p = (2.0f * l) - q;

			float Hk = h;
			float[] T = new float[3];
			T[0] = Hk + (1.0f / 3.0f);	// Tr
			T[1] = Hk;				    // Tb
			T[2] = Hk - (1.0f / 3.0f);	// Tg

			for (int i = 0; i < 3; i++)
			{
				if (T[i] < 0) 
					T[i] += 1.0;
				
				if (T[i] > 1) 
					T[i] -= 1.0;

				if ((T[i] * 6) < 1)
				{
					T[i] = p + ((q - p) * 6.0f * T[i]);
				}
				else if ((T[i] * 2.0f) < 1)
				{
					T[i] = q;
				}
				else if ((T[i] * 3.0f) < 2)
				{
					T[i] = p + (q - p) * ((2.0f / 3.0f) - T[i]) * 6.0f;
				}
				else T[i] = p;
			}

			result[0] = T[0];
			result[1] = T[1];
			result[2] = T[2];
			result[3] = a;
		}
		
		return result;
	}
}
