package com.ajoy.etol.mapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ajoy.etol.config.CharSequenceToCodePointMapping;
import com.ajoy.etol.config.TransliteratorConfig;

/**
 * 
 * @author kalyanc
 *
 */
public class DefaultCharSequenceMapper implements EnglishCharSequenceToLanguageMapper, LanguageToEnglishCharSequenceMapper
{		
	private static Logger log = LogManager.getLogger(DefaultCharSequenceMapper.class);
	
	private TransliteratorConfig config; 

	/** All these are populated from the config */
	private CharSequenceToCodePointMapping[] symbolList ;	
	private int visarga;
	private CharSequenceToCodePointMapping visargaSymbol;
	
	private Map<String, CharSequenceToCodePointMapping> codepointMap = new HashMap<String, CharSequenceToCodePointMapping>();
	
	private Map<String, CharSequenceToCodePointMapping> consonantMap = new HashMap<String, CharSequenceToCodePointMapping>();
	private Map<String, CharSequenceToCodePointMapping> vowelMap = new HashMap<String, CharSequenceToCodePointMapping>();
	private Map<String, CharSequenceToCodePointMapping> commentMap = new HashMap<String, CharSequenceToCodePointMapping>();
	
	
	private boolean[] listOfAsciiCharUsedForComments = new boolean[128];
	private boolean[] listOfAsciiCharUsedForLanguageVowel = new boolean[128];
	private boolean[] listOfAsciiCharUsedForLanguageConsonant = new boolean[128];
	
	public DefaultCharSequenceMapper()
	{				
		for(int i=0 ; listOfAsciiCharUsedForLanguageConsonant.length < 128; i++)
			listOfAsciiCharUsedForLanguageConsonant[i] = false;

		for(int i=0 ; listOfAsciiCharUsedForLanguageVowel.length < 128; i++)					
				listOfAsciiCharUsedForLanguageVowel[i] = false;
				
		init();		
		populateFlags();		
	}
	
	public CharSequenceToCodePointMapping getAsciSequence(int codepoint)
	{
		return codepointMap.get(codepoint+"");
	}
	
	public void init()
	{
		try
		{
			InputStream in = DefaultCharSequenceMapper.class.getResourceAsStream("/telugu-language-symbols.xml");
			config = TransliteratorConfig.getFromStream(in);			
			visarga = Integer.parseInt(config.getVisarga(), 16);	
			visargaSymbol = new CharSequenceToCodePointMapping();
			int val[] = new int[1];
			val[0] = visarga;
			visargaSymbol.setCodepoints(val);
			visargaSymbol.setHexCodepoint(config.getVisarga());
			
			codepointMap.put(visarga+"", visargaSymbol);
			
			symbolList = new CharSequenceToCodePointMapping[config.getSymbolsList().size()];						
			int i = 0;			
			for(CharSequenceToCodePointMapping sy: config.getSymbolsList())
			{	
				String [] cps = sy.getHexCodepoint().split(",");
				
				log.debug("cps[" + cps.length + "] = " + cps);

				int[] cpInts = new int[cps.length];
				for (int j = 0; j < cps.length; j++) 
					cpInts[j] = Integer.parseInt(cps[j], 16);
				
				sy.setCodepoints(cpInts);

				if(log.isDebugEnabled())
				{
					StringBuilder buff = new StringBuilder();
					buff.append(sy.getAsciiCharSequence() + " = ");
					for (int j = 0; j < cpInts.length; j++)
						buff.append(cpInts[j] + ",");
					buff.append("\n");
				}
				
				if(sy.getCodepoints().length == 1)
					codepointMap.put(sy.getCodepoints()[0]+"", sy);
				
				if(sy.getAsciiCharSequence() == null)
				{
					log.warn("This is most wierd thing. Hard coding 'kh' in code");
					sy.setAsciiCharSequence("kh");
				}

				log.info(" Type: "+sy.getType()+" DEC: "+sy.getCodepoints()[0]+" HEX: "+sy.getHexCodepoint()+" Seq: "+sy.getAsciiCharSequence());
						
				symbolList[i] = sy;												
				if(sy.getType() == CharSequenceToCodePointMapping.Consonant)
				{					
					consonantMap.put(sy.getAsciiCharSequence(), sy);
				}
				else if(sy.getType() == CharSequenceToCodePointMapping.Vowel)
				{
					vowelMap.put(sy.getAsciiCharSequence(), sy);
				}
				else if(sy.getType() == CharSequenceToCodePointMapping.ConsonantModification)
				{
					consonantMap.put(sy.getAsciiCharSequence(), sy);
				}
				else if(sy.getType() == CharSequenceToCodePointMapping.CommentChar)
				{
					commentMap.put(sy.getAsciiCharSequence(), sy);
				}				
				else
				{
					throw new IllegalStateException("Invalid type: "+sy);
				}		
								
				i++;
			}
			
			TransliteratorConfig.writeToStream(new FileOutputStream(new File("./symbols.xml")), config);
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
	}
	
	private void populateFlags()
	{		
		for(int i=0; i<config.getConsonantCharList().size(); i++)
		{							
			int index = (int) config.getConsonantCharList().get(i).charAt(0);
			listOfAsciiCharUsedForLanguageConsonant[index] = true;
		}			
		for(int i=0; i<config.getVowelCharList().size(); i++)
		{							
			int index = (int) config.getVowelCharList().get(i).charAt(0);
			listOfAsciiCharUsedForLanguageVowel[index] = true;
		}	

		if(config.getMarkupStart() != null)
		{
			for(char c: config.getMarkupStart().toCharArray())
				listOfAsciiCharUsedForComments[c] = true;
		}

		if(config.getMarkupEnd() != null)
		{
			for(char c: config.getMarkupEnd().toCharArray())
				listOfAsciiCharUsedForComments[c] = true;
		}

	}
	
	public String getMarkupStart()
	{
		return config.getMarkupStart();
	}
	
	public String getMarkupEnd()
	{
		return config.getMarkupEnd();
	}

	
	public CharSequenceToCodePointMapping getVowelSymbol(String asciiCharSequence) 
	{
		
		log.debug("vowl-key: "+asciiCharSequence);
		CharSequenceToCodePointMapping sy = vowelMap.get(asciiCharSequence);		
		if(sy == null)
			log.debug("Not able to find vowel symbol for key: "+asciiCharSequence);
		
		return sy;
	}

	public CharSequenceToCodePointMapping getConsonantSymbol(String asciiCharSequence) 
	{		
		log.debug("cons-key: "+asciiCharSequence);
		CharSequenceToCodePointMapping sy = consonantMap.get(asciiCharSequence);		
		if(sy == null)
			log.debug("Not able to find consonant symbol for key: "+asciiCharSequence);
		
		return sy;
	}

	
	public CharSequenceToCodePointMapping getMarkupSymbol(String asciiCharSequence) 
	{		
		log.debug("comm-key: "+asciiCharSequence);
		CharSequenceToCodePointMapping sy = commentMap.get(asciiCharSequence);		
		if(sy == null)
			log.debug("Not able to find comment symbol for key: "+asciiCharSequence);
		
		return sy;
	}

	
	public int getVisarga()
	{
		return visarga;
	}

	public CharSequenceToCodePointMapping getVisargaSymbol()
	{
		return visargaSymbol;
	}

	
	public CharSequenceToCodePointMapping[] getSymbolList() 
	{
		return symbolList;
	}

	public boolean isTransliterationMarkedup()	
	{
		return config.isTransliterationMarkedup();
	}
	
	public boolean isAsciiCharUsedForLanguageVowel(int c)
	{
		if(c > listOfAsciiCharUsedForLanguageVowel.length - 1)
			return false;
					
		return  listOfAsciiCharUsedForLanguageVowel[c];
	}
	
	public boolean isAsciiCharUsedForLanguageConsonant(int c)
	{
		if(c > listOfAsciiCharUsedForLanguageConsonant.length - 1)
			return false;
		
		return  listOfAsciiCharUsedForLanguageConsonant[c];
	}

	public boolean isAsciiCharPassThrough(int c)
	{
		return !( isAsciiCharUsedForLanguageConsonant(c) || isAsciiCharUsedForLanguageVowel(c));
	}
	
	public boolean isAsciiCharUsedInMarkup(int c)
	{
		if(c > listOfAsciiCharUsedForComments.length -1)
			return false;
		
		return listOfAsciiCharUsedForComments[c];
	}
}
